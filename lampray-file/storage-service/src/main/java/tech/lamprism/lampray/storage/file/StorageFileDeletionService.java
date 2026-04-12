/*
 * Copyright (C) 2023-2026 RollW
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.lamprism.lampray.storage.file;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.backend.BlobStoreLocator;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementRepository;
import tech.lamprism.lampray.storage.persistence.StorageBlobRepository;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.support.StorageBlobLifecycleLockManager;
import tech.rollw.common.web.AuthErrorCode;
import tech.rollw.common.web.DataErrorCode;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class StorageFileDeletionService {
    private final StorageFileRepository storageFileRepository;
    private final StorageBlobRepository storageBlobRepository;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final BlobStoreLocator blobStoreLocator;
    private final StorageBlobLifecycleLockManager storageBlobLifecycleLockManager;
    private final StorageBlobCleanupRouter storageBlobCleanupRouter;
    private final StorageRuntimeConfig storageRuntimeConfig;
    private final TransactionTemplate transactionTemplate;

    public StorageFileDeletionService(StorageFileRepository storageFileRepository,
                                      StorageBlobRepository storageBlobRepository,
                                      StorageBlobPlacementRepository storageBlobPlacementRepository,
                                      StorageUploadSessionRepository storageUploadSessionRepository,
                                      BlobStoreLocator blobStoreLocator,
                                      StorageBlobLifecycleLockManager storageBlobLifecycleLockManager,
                                      StorageBlobCleanupRouter storageBlobCleanupRouter,
                                      StorageRuntimeConfig storageRuntimeConfig,
                                      PlatformTransactionManager transactionManager) {
        this.storageFileRepository = storageFileRepository;
        this.storageBlobRepository = storageBlobRepository;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.blobStoreLocator = blobStoreLocator;
        this.storageBlobLifecycleLockManager = storageBlobLifecycleLockManager;
        this.storageBlobCleanupRouter = storageBlobCleanupRouter;
        this.storageRuntimeConfig = storageRuntimeConfig;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void deleteFile(String fileId,
                           Long userId) throws IOException {
        try (StorageBlobLifecycleLockManager.LockedKey ignored = storageBlobLifecycleLockManager.acquire(resolveLockKey(fileId))) {
            StorageBlobCleanupRouter.StorageBlobCleanupPlan cleanupPlan = Objects.requireNonNull(
                    transactionTemplate.execute(status -> deleteFileInTransaction(fileId, userId))
            );
            cleanupOrphanedObjects(cleanupPlan);
        }
    }

    private StorageBlobCleanupRouter.StorageBlobCleanupPlan deleteFileInTransaction(String fileId,
                                                                                    Long userId) {
        StorageFileEntity fileEntity = requireOwnedFile(fileId, userId);
        String blobId = fileEntity.getBlobId();
        StorageBlobEntity blobEntity = storageBlobRepository.findById(blobId).orElse(null);
        OffsetDateTime now = OffsetDateTime.now();

        StorageFileEntity deletedFile = fileEntity.toBuilder()
                .setDeleted(true)
                .setUpdateTime(now)
                .build();
        storageFileRepository.save(deletedFile);
        storageFileRepository.flush();
        if (storageFileRepository.existsActiveByBlobId(blobId)) {
            return StorageBlobCleanupRouter.StorageBlobCleanupPlan.empty();
        }

        List<StorageBlobPlacementEntity> placements = storageBlobPlacementRepository.findAllByBlobId(blobId);
        if (blobEntity == null) {
            if (placements.isEmpty()) {
                return StorageBlobCleanupRouter.StorageBlobCleanupPlan.empty();
            }
            markPlacementsDeleted(placements, now);
            return storageBlobCleanupRouter.routePlacements(placements);
        }

        markPlacementsDeleted(placements, now);
        markBlobOrphaned(blobEntity, now);
        return StorageBlobCleanupRouter.StorageBlobCleanupPlan.empty();
    }

    private void cleanupOrphanedObjects(StorageBlobCleanupRouter.StorageBlobCleanupPlan cleanupPlan) throws IOException {
        for (StorageBlobCleanupRouter.StorageBlobCleanupTarget target : cleanupPlan.targets()) {
            if (isStillReferenced(target.backendName(), target.objectKey())) {
                continue;
            }
            blobStoreLocator.require(target.backendName()).delete(target.objectKey());
        }
    }

    private boolean isStillReferenced(String backendName,
                                      String objectKey) {
        return storageBlobRepository.existsByPrimaryBackendAndPrimaryObjectKey(backendName, objectKey)
                || storageBlobPlacementRepository.existsByBackendNameAndObjectKey(backendName, objectKey)
                || storageUploadSessionRepository.existsPendingSessionByPrimaryBackendAndObjectKey(backendName, objectKey);
    }

    private StorageFileEntity requireOwnedFile(String fileId,
                                               Long userId) {
        StorageFileEntity fileEntity = storageFileRepository.findActiveById(fileId)
                .orElseThrow(() -> new StorageException(
                        DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "File not found: " + fileId
                ));
        if (userId == null || !userId.equals(fileEntity.getOwnerUserId())) {
            throw new StorageException(
                    AuthErrorCode.ERROR_UNAUTHORIZED_USE,
                    "You are not allowed to delete this file."
            );
        }
        return fileEntity;
    }

    public int purgeRetainedBlobs(OffsetDateTime now) throws IOException {
        OffsetDateTime retentionCutoff = resolveRetentionCutoff(now);
        int purged = 0;
        for (StorageBlobEntity candidate : storageBlobRepository.findAllByOrphanedAtBefore(retentionCutoff)) {
            if (purgeRetainedBlob(candidate.getBlobId(), candidate.getContentChecksum(), retentionCutoff)) {
                purged++;
            }
        }
        return purged;
    }

    private boolean purgeRetainedBlob(String blobId,
                                      String lockKey,
                                      OffsetDateTime retentionCutoff) throws IOException {
        try (StorageBlobLifecycleLockManager.LockedKey ignored = storageBlobLifecycleLockManager.acquire(lockKey)) {
            BlobPurgePlan purgePlan = Objects.requireNonNull(
                    transactionTemplate.execute(status -> purgeRetainedBlobInTransaction(blobId, retentionCutoff))
            );
            if (!purgePlan.ready()) {
                return false;
            }
            if (!cleanupRetainedBlobObjects(blobId, purgePlan.cleanupPlan())) {
                return false;
            }
            return Objects.requireNonNull(
                    transactionTemplate.execute(status -> finalizeRetainedBlobPurge(blobId, retentionCutoff))
            );
        }
    }

    private BlobPurgePlan purgeRetainedBlobInTransaction(String blobId,
                                                         OffsetDateTime retentionCutoff) {
        StorageBlobEntity blobEntity = storageBlobRepository.findById(blobId).orElse(null);
        if (blobEntity == null || blobEntity.getOrphanedAt() == null || blobEntity.getOrphanedAt().isAfter(retentionCutoff)) {
            return BlobPurgePlan.notReady();
        }
        if (storageFileRepository.existsActiveByBlobId(blobId)) {
            StorageBlobEntity restoredBlob = blobEntity.toBuilder()
                    .setOrphanedAt(null)
                    .setUpdateTime(OffsetDateTime.now())
                    .build();
            storageBlobRepository.save(restoredBlob);
            return BlobPurgePlan.notReady();
        }
        List<StorageBlobPlacementEntity> placements = storageBlobPlacementRepository.findAllIncludingDeletedByBlobId(blobId);
        return BlobPurgePlan.ready(storageBlobCleanupRouter.route(blobEntity, placements));
    }

    private boolean cleanupRetainedBlobObjects(String blobId,
                                               StorageBlobCleanupRouter.StorageBlobCleanupPlan cleanupPlan) throws IOException {
        for (StorageBlobCleanupRouter.StorageBlobCleanupTarget target : cleanupPlan.targets()) {
            if (isStillReferencedForPurge(blobId, target.backendName(), target.objectKey())) {
                continue;
            }
            var blobStore = blobStoreLocator.require(target.backendName());
            if (!blobStore.exists(target.objectKey())) {
                continue;
            }
            if (!blobStore.delete(target.objectKey()) && blobStore.exists(target.objectKey())) {
                return false;
            }
        }
        return true;
    }

    private boolean finalizeRetainedBlobPurge(String blobId,
                                              OffsetDateTime retentionCutoff) {
        StorageBlobEntity blobEntity = storageBlobRepository.findById(blobId).orElse(null);
        if (blobEntity == null || blobEntity.getOrphanedAt() == null || blobEntity.getOrphanedAt().isAfter(retentionCutoff)) {
            return false;
        }
        if (storageFileRepository.existsActiveByBlobId(blobId)) {
            StorageBlobEntity restoredBlob = blobEntity.toBuilder()
                    .setOrphanedAt(null)
                    .setUpdateTime(OffsetDateTime.now())
                    .build();
            storageBlobRepository.save(restoredBlob);
            return false;
        }
        if (!storageBlobPlacementRepository.findAllByBlobId(blobId).isEmpty()) {
            return false;
        }
        OffsetDateTime purgedAt = OffsetDateTime.now();
        markFilesPurged(storageFileRepository.findDeletedByBlobId(blobId), purgedAt);
        markPlacementsPurged(storageBlobPlacementRepository.findAllIncludingDeletedByBlobId(blobId), purgedAt);
        storageBlobRepository.delete(blobEntity);
        return true;
    }

    private boolean isStillReferencedForPurge(String blobId,
                                              String backendName,
                                              String objectKey) {
        return storageBlobRepository.existsOtherByPrimaryBackendAndPrimaryObjectKey(backendName, objectKey, blobId)
                || storageBlobPlacementRepository.existsByBackendNameAndObjectKey(backendName, objectKey)
                || storageUploadSessionRepository.existsPendingSessionByPrimaryBackendAndObjectKey(backendName, objectKey);
    }

    private void markPlacementsDeleted(List<StorageBlobPlacementEntity> placements,
                                       OffsetDateTime now) {
        if (placements.isEmpty()) {
            return;
        }
        List<StorageBlobPlacementEntity> updatedPlacements = placements.stream()
                .map(placement -> placement.toBuilder()
                        .setDeleted(true)
                        .setUpdateTime(now)
                        .build())
                .toList();
        storageBlobPlacementRepository.saveAll(updatedPlacements);
    }

    private void markPlacementsPurged(List<StorageBlobPlacementEntity> placements,
                                      OffsetDateTime purgedAt) {
        if (placements.isEmpty()) {
            return;
        }
        List<StorageBlobPlacementEntity> purgedPlacements = placements.stream()
                .map(placement -> placement.toBuilder()
                        .setPurgedAt(purgedAt)
                        .setUpdateTime(purgedAt)
                        .build())
                .toList();
        storageBlobPlacementRepository.saveAll(purgedPlacements);
    }

    private void markFilesPurged(List<StorageFileEntity> files,
                                 OffsetDateTime purgedAt) {
        if (files.isEmpty()) {
            return;
        }
        List<StorageFileEntity> purgedFiles = files.stream()
                .map(file -> file.toBuilder()
                        .setPurgedAt(purgedAt)
                        .setUpdateTime(purgedAt)
                        .build())
                .toList();
        storageFileRepository.saveAll(purgedFiles);
    }

    private void markBlobOrphaned(StorageBlobEntity blobEntity,
                                  OffsetDateTime now) {
        StorageBlobEntity orphanedBlob = blobEntity.toBuilder()
                .setOrphanedAt(now)
                .setUpdateTime(now)
                .build();
        storageBlobRepository.save(orphanedBlob);
    }

    private OffsetDateTime resolveRetentionCutoff(OffsetDateTime now) {
        long retentionSeconds = storageRuntimeConfig.getCleanupDeletedBlobRetainSeconds();
        if (retentionSeconds <= 0) {
            return now;
        }
        return now.minusSeconds(retentionSeconds);
    }

    private String resolveLockKey(String fileId) {
        return storageFileRepository.findActiveById(fileId)
                .flatMap(fileEntity -> storageBlobRepository.findById(fileEntity.getBlobId())
                        .map(StorageBlobEntity::getContentChecksum)
                        .or(() -> java.util.Optional.of(fileEntity.getBlobId())))
                .orElse(fileId);
    }

    private record BlobPurgePlan(boolean ready,
                                 StorageBlobCleanupRouter.StorageBlobCleanupPlan cleanupPlan) {
        private static BlobPurgePlan notReady() {
            return new BlobPurgePlan(false, StorageBlobCleanupRouter.StorageBlobCleanupPlan.empty());
        }

        private static BlobPurgePlan ready(StorageBlobCleanupRouter.StorageBlobCleanupPlan cleanupPlan) {
            return new BlobPurgePlan(true, cleanupPlan);
        }
    }
}
