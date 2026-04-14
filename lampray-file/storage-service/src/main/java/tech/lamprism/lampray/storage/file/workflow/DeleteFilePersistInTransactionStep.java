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

package tech.lamprism.lampray.storage.file.workflow;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementRepository;
import tech.lamprism.lampray.storage.persistence.StorageBlobRepository;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.routing.StorageGroupRouter;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;
import tech.rollw.common.web.AuthErrorCode;
import tech.rollw.common.web.DataErrorCode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class DeleteFilePersistInTransactionStep implements WorkflowStep<DeleteFileWorkflowContext> {
    private static final int STEP_ORDER = 100;

    private final StorageFileRepository storageFileRepository;
    private final StorageBlobRepository storageBlobRepository;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final StorageGroupRouter storageGroupRouter;
    private final TransactionTemplate transactionTemplate;

    public DeleteFilePersistInTransactionStep(StorageFileRepository storageFileRepository,
                                              StorageBlobRepository storageBlobRepository,
                                              StorageBlobPlacementRepository storageBlobPlacementRepository,
                                              StorageGroupRouter storageGroupRouter,
                                              PlatformTransactionManager transactionManager) {
        this.storageFileRepository = storageFileRepository;
        this.storageBlobRepository = storageBlobRepository;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.storageGroupRouter = storageGroupRouter;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public int getOrder() {
        return STEP_ORDER;
    }

    @Override
    public void execute(DeleteFileWorkflowContext context) {
        var cleanupPlan = Objects.requireNonNull(
                transactionTemplate.execute(status -> deleteFileInTransaction(context)),
                "deleteFileCleanupPlan"
        );
        context.getState().setCleanupPlan(cleanupPlan);
    }

    private StorageGroupRouter.StorageBlobCleanupPlan deleteFileInTransaction(DeleteFileWorkflowContext context) {
        StorageFileEntity fileEntity = requireOwnedFile(context.getFileId(), context.getUserId());
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
            return StorageGroupRouter.StorageBlobCleanupPlan.empty();
        }

        List<StorageBlobPlacementEntity> placements = storageBlobPlacementRepository.findAllByBlobId(blobId);
        if (blobEntity == null) {
            if (placements.isEmpty()) {
                return StorageGroupRouter.StorageBlobCleanupPlan.empty();
            }
            markPlacementsDeleted(placements, now);
            return storageGroupRouter.routeCleanup(placements);
        }

        markPlacementsDeleted(placements, now);
        markBlobOrphaned(blobEntity, now);
        return StorageGroupRouter.StorageBlobCleanupPlan.empty();
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

    private void markBlobOrphaned(StorageBlobEntity blobEntity,
                                  OffsetDateTime now) {
        StorageBlobEntity orphanedBlob = blobEntity.toBuilder()
                .setOrphanedAt(now)
                .setUpdateTime(now)
                .build();
        storageBlobRepository.save(orphanedBlob);
    }
}
