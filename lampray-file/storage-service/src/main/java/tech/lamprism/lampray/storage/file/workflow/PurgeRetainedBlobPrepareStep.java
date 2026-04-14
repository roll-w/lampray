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
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.routing.StorageGroupRouter;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class PurgeRetainedBlobPrepareStep implements WorkflowStep<PurgeRetainedBlobWorkflowContext> {
    private static final int STEP_ORDER = 100;

    private final StorageBlobRepository storageBlobRepository;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final StorageFileRepository storageFileRepository;
    private final StorageGroupRouter storageGroupRouter;
    private final TransactionTemplate transactionTemplate;

    public PurgeRetainedBlobPrepareStep(StorageBlobRepository storageBlobRepository,
                                        StorageBlobPlacementRepository storageBlobPlacementRepository,
                                        StorageFileRepository storageFileRepository,
                                        StorageGroupRouter storageGroupRouter,
                                        PlatformTransactionManager transactionManager) {
        this.storageBlobRepository = storageBlobRepository;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.storageFileRepository = storageFileRepository;
        this.storageGroupRouter = storageGroupRouter;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public int getOrder() {
        return STEP_ORDER;
    }

    @Override
    public void execute(PurgeRetainedBlobWorkflowContext context) {
        BlobPurgePlan purgePlan = Objects.requireNonNull(
                transactionTemplate.execute(status -> preparePurgePlan(context)),
                "purgeRetainedBlobPlan"
        );
        context.getState().setReady(purgePlan.ready());
        context.getState().setCleanupPlan(purgePlan.cleanupPlan());
    }

    private BlobPurgePlan preparePurgePlan(PurgeRetainedBlobWorkflowContext context) {
        StorageBlobEntity blobEntity = storageBlobRepository.findById(context.getBlobId()).orElse(null);
        if (blobEntity == null || blobEntity.getOrphanedAt() == null || blobEntity.getOrphanedAt().isAfter(context.getRetentionCutoff())) {
            return BlobPurgePlan.notReady();
        }
        if (storageFileRepository.existsActiveByBlobId(context.getBlobId())) {
            StorageBlobEntity restoredBlob = blobEntity.toBuilder()
                    .setOrphanedAt(null)
                    .setUpdateTime(OffsetDateTime.now())
                    .build();
            storageBlobRepository.save(restoredBlob);
            return BlobPurgePlan.notReady();
        }
        List<StorageBlobPlacementEntity> placements = storageBlobPlacementRepository.findAllIncludingDeletedByBlobId(context.getBlobId());
        return BlobPurgePlan.ready(storageGroupRouter.routeCleanup(blobEntity, placements));
    }

    private record BlobPurgePlan(boolean ready,
                                 StorageGroupRouter.StorageBlobCleanupPlan cleanupPlan) {
        private static BlobPurgePlan notReady() {
            return new BlobPurgePlan(false, StorageGroupRouter.StorageBlobCleanupPlan.empty());
        }

        private static BlobPurgePlan ready(StorageGroupRouter.StorageBlobCleanupPlan cleanupPlan) {
            return new BlobPurgePlan(true, cleanupPlan);
        }
    }
}
