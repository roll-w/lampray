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
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementRepository;
import tech.lamprism.lampray.storage.persistence.StorageBlobRepository;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class PurgeRetainedBlobFinalizeStep implements WorkflowStep<PurgeRetainedBlobWorkflowContext> {
    private static final int STEP_ORDER = 300;

    private final StorageBlobRepository storageBlobRepository;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final StorageFileRepository storageFileRepository;
    private final TransactionTemplate transactionTemplate;

    public PurgeRetainedBlobFinalizeStep(StorageBlobRepository storageBlobRepository,
                                         StorageBlobPlacementRepository storageBlobPlacementRepository,
                                         StorageFileRepository storageFileRepository,
                                         PlatformTransactionManager transactionManager) {
        this.storageBlobRepository = storageBlobRepository;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.storageFileRepository = storageFileRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public int getOrder() {
        return STEP_ORDER;
    }

    @Override
    public void execute(PurgeRetainedBlobWorkflowContext context) {
        if (!context.getState().getReady()) {
            return;
        }
        boolean finalized = Objects.requireNonNull(
                transactionTemplate.execute(status -> finalizePurge(context)),
                "purgeRetainedBlobResult"
        );
        context.getState().setResult(finalized);
    }

    private boolean finalizePurge(PurgeRetainedBlobWorkflowContext context) {
        StorageBlobEntity blobEntity = storageBlobRepository.findById(context.getBlobId()).orElse(null);
        if (blobEntity == null || blobEntity.getOrphanedAt() == null || blobEntity.getOrphanedAt().isAfter(context.getRetentionCutoff())) {
            return false;
        }
        if (storageFileRepository.existsActiveByBlobId(context.getBlobId())) {
            StorageBlobEntity restoredBlob = blobEntity.toBuilder()
                    .setOrphanedAt(null)
                    .setUpdateTime(OffsetDateTime.now())
                    .build();
            storageBlobRepository.save(restoredBlob);
            return false;
        }
        if (!storageBlobPlacementRepository.findAllByBlobId(context.getBlobId()).isEmpty()) {
            return false;
        }
        OffsetDateTime purgedAt = OffsetDateTime.now();
        markFilesPurged(storageFileRepository.findDeletedByBlobId(context.getBlobId()), purgedAt);
        markPlacementsPurged(storageBlobPlacementRepository.findAllIncludingDeletedByBlobId(context.getBlobId()), purgedAt);
        storageBlobRepository.delete(blobEntity);
        return true;
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
}
