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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class StorageFileDeletionService {
    // TODO: split to Router
    private final StorageFileRepository storageFileRepository;
    private final StorageBlobRepository storageBlobRepository;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final BlobStoreLocator blobStoreLocator;
    private final StorageBlobLifecycleLockManager storageBlobLifecycleLockManager;
    private final StorageRuntimeConfig storageRuntimeConfig;
    private final TransactionTemplate transactionTemplate;

    public StorageFileDeletionService(StorageFileRepository storageFileRepository,
                                      StorageBlobRepository storageBlobRepository,
                                      StorageBlobPlacementRepository storageBlobPlacementRepository,
                                      StorageUploadSessionRepository storageUploadSessionRepository,
                                      BlobStoreLocator blobStoreLocator,
                                      StorageBlobLifecycleLockManager storageBlobLifecycleLockManager,
                                      StorageRuntimeConfig storageRuntimeConfig,
                                      PlatformTransactionManager transactionManager) {
        this.storageFileRepository = storageFileRepository;
        this.storageBlobRepository = storageBlobRepository;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.blobStoreLocator = blobStoreLocator;
        this.storageBlobLifecycleLockManager = storageBlobLifecycleLockManager;
        this.storageRuntimeConfig = storageRuntimeConfig;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void deleteFile(String fileId,
                           Long userId) throws IOException {
        try (StorageBlobLifecycleLockManager.LockedKey ignored = storageBlobLifecycleLockManager.acquire(resolveLockKey(fileId))) {
            BlobCleanupPlan cleanupPlan = Objects.requireNonNull(
                    transactionTemplate.execute(status -> deleteFileInTransaction(fileId, userId))
            );
            cleanupOrphanedObjects(cleanupPlan);
        }
    }

    private BlobCleanupPlan deleteFileInTransaction(String fileId,
                                                    Long userId) {
        StorageFileEntity fileEntity = requireOwnedFile(fileId, userId);
        String blobId = fileEntity.getBlobId();
        StorageBlobEntity blobEntity = storageBlobRepository.lockById(blobId).orElse(null);

        storageFileRepository.delete(fileEntity);
        storageFileRepository.flush();
        if (storageFileRepository.existsByBlobId(blobId)) {
            return BlobCleanupPlan.empty();
        }

        List<StorageBlobPlacementEntity> placements = storageBlobPlacementRepository.findAllByBlobId(blobId);
        if (blobEntity == null) {
            if (placements.isEmpty()) {
                return BlobCleanupPlan.empty();
            }
            storageBlobPlacementRepository.deleteAll(placements);
            return BlobCleanupPlan.fromPlacements(placements);
        }

        if (storageRuntimeConfig.getCleanupDeletedBlobRetainSeconds() > 0) {
            markBlobOrphaned(blobEntity, OffsetDateTime.now());
            return BlobCleanupPlan.empty();
        }

        storageBlobPlacementRepository.deleteAll(placements);
        storageBlobRepository.delete(blobEntity);
        if (placements.isEmpty()) {
            return BlobCleanupPlan.from(blobEntity, List.of());
        }
        return BlobCleanupPlan.from(blobEntity, placements);
    }

    private void cleanupOrphanedObjects(BlobCleanupPlan cleanupPlan) throws IOException {
        for (StoredBlobObject target : cleanupPlan.targets()) {
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
        StorageFileEntity fileEntity = storageFileRepository.findById(fileId)
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
        long retentionSeconds = storageRuntimeConfig.getCleanupDeletedBlobRetainSeconds();
        if (retentionSeconds <= 0) {
            return 0;
        }
        OffsetDateTime retentionCutoff = now.minusSeconds(retentionSeconds);
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
            BlobPurgeResult purgeResult = Objects.requireNonNull(
                    transactionTemplate.execute(status -> purgeRetainedBlobInTransaction(blobId, retentionCutoff))
            );
            if (!purgeResult.purged()) {
                return false;
            }
            cleanupOrphanedObjects(purgeResult.cleanupPlan());
            return true;
        }
    }

    private BlobPurgeResult purgeRetainedBlobInTransaction(String blobId,
                                                           OffsetDateTime retentionCutoff) {
        StorageBlobEntity blobEntity = storageBlobRepository.lockById(blobId).orElse(null);
        if (blobEntity == null || blobEntity.getOrphanedAt() == null || blobEntity.getOrphanedAt().isAfter(retentionCutoff)) {
            return BlobPurgeResult.notPurged();
        }
        if (storageFileRepository.existsByBlobId(blobId)) {
            blobEntity.setOrphanedAt(null);
            blobEntity.setUpdateTime(OffsetDateTime.now());
            storageBlobRepository.save(blobEntity);
            return BlobPurgeResult.notPurged();
        }
        List<StorageBlobPlacementEntity> placements = storageBlobPlacementRepository.findAllByBlobId(blobId);
        storageBlobPlacementRepository.deleteAll(placements);
        storageBlobRepository.delete(blobEntity);
        return BlobPurgeResult.purged(BlobCleanupPlan.from(blobEntity, placements));
    }

    private void markBlobOrphaned(StorageBlobEntity blobEntity,
                                  OffsetDateTime now) {
        blobEntity.setOrphanedAt(now);
        blobEntity.setUpdateTime(now);
        storageBlobRepository.save(blobEntity);
    }

    private String resolveLockKey(String fileId) {
        return storageFileRepository.findById(fileId)
                .flatMap(fileEntity -> storageBlobRepository.findById(fileEntity.getBlobId())
                        .map(StorageBlobEntity::getContentChecksum)
                        .or(() -> java.util.Optional.of(fileEntity.getBlobId())))
                .orElse(fileId);
    }

    private record StoredBlobObject(String backendName,
                                    String objectKey) {
    }

    private record BlobCleanupPlan(List<StoredBlobObject> targets) {
        private static BlobCleanupPlan empty() {
            return new BlobCleanupPlan(List.of());
        }

        private static BlobCleanupPlan from(StorageBlobEntity blobEntity,
                                            List<StorageBlobPlacementEntity> placements) {
            Map<String, Set<String>> targets = new LinkedHashMap<>();
            addTarget(targets, blobEntity.getPrimaryBackend(), blobEntity.getPrimaryObjectKey());
            for (StorageBlobPlacementEntity placement : placements) {
                addTarget(targets, placement.getBackendName(), placement.getObjectKey());
            }
            return fromTargetMap(targets);
        }

        private static BlobCleanupPlan fromPlacements(List<StorageBlobPlacementEntity> placements) {
            Map<String, Set<String>> targets = new LinkedHashMap<>();
            for (StorageBlobPlacementEntity placement : placements) {
                addTarget(targets, placement.getBackendName(), placement.getObjectKey());
            }
            return fromTargetMap(targets);
        }

        private static BlobCleanupPlan fromTargetMap(Map<String, Set<String>> targets) {
            List<StoredBlobObject> cleanupTargets = new ArrayList<>();
            for (Map.Entry<String, Set<String>> entry : targets.entrySet()) {
                for (String objectKey : entry.getValue()) {
                    cleanupTargets.add(new StoredBlobObject(entry.getKey(), objectKey));
                }
            }
            return new BlobCleanupPlan(List.copyOf(cleanupTargets));
        }

        private static void addTarget(Map<String, Set<String>> targets,
                                      String backendName,
                                      String objectKey) {
            targets.computeIfAbsent(backendName, ignored -> new LinkedHashSet<>()).add(objectKey);
        }
    }

    private record BlobPurgeResult(boolean purged,
                                   BlobCleanupPlan cleanupPlan) {
        private static BlobPurgeResult notPurged() {
            return new BlobPurgeResult(false, BlobCleanupPlan.empty());
        }

        private static BlobPurgeResult purged(BlobCleanupPlan cleanupPlan) {
            return new BlobPurgeResult(true, cleanupPlan);
        }
    }
}
