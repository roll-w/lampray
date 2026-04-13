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
import tech.lamprism.lampray.common.data.ResourceIdGenerator;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.StorageResourceKind;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageTopology;
import tech.lamprism.lampray.storage.file.PersistedMaterialization;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementRepository;
import tech.lamprism.lampray.storage.persistence.StorageBlobRepository;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class PersistSessionUploadPersistInTransactionStep implements WorkflowStep<PersistSessionUploadWorkflowContext> {
    private static final int STEP_ORDER = 100;

    private final StorageBlobRepository storageBlobRepository;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final StorageFileRepository storageFileRepository;
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final StorageTopology storageTopology;
    private final ResourceIdGenerator resourceIdGenerator;
    private final TransactionTemplate transactionTemplate;

    PersistSessionUploadPersistInTransactionStep(StorageBlobRepository storageBlobRepository,
                                                 StorageBlobPlacementRepository storageBlobPlacementRepository,
                                                  StorageFileRepository storageFileRepository,
                                                  StorageUploadSessionRepository storageUploadSessionRepository,
                                                  StorageTopology storageTopology,
                                                 ResourceIdGenerator resourceIdGenerator,
                                                  PlatformTransactionManager transactionManager) {
        this.storageBlobRepository = storageBlobRepository;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.storageFileRepository = storageFileRepository;
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.storageTopology = storageTopology;
        this.resourceIdGenerator = resourceIdGenerator;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public int getOrder() {
        return STEP_ORDER;
    }

    @Override
    public void execute(PersistSessionUploadWorkflowContext context) {
        PersistedMaterialization persistedMaterialization = Objects.requireNonNull(
                transactionTemplate.execute(status -> persistSessionUpload(context)),
                "persistedMaterialization"
        );
        context.getState().setPersistedMaterialization(persistedMaterialization);
        context.getState().setResult(persistedMaterialization.getFileStorage());
    }

    private PersistedMaterialization persistSessionUpload(PersistSessionUploadWorkflowContext context) {
        OffsetDateTime now = OffsetDateTime.now();
        StorageBlobEntity blobEntity = storageBlobRepository.materialize(context.getPreparedBlob(), newBlobId(), now);
        storageBlobPlacementRepository.syncPlacements(
                blobEntity.getBlobId(),
                context.getPreparedBlob().getPlacementsToPersist(),
                now
        );
        StorageGroupConfig groupConfig = storageTopology.getGroup(context.getUploadSession().getGroupName());
        StorageFileEntity fileEntity = StorageFileEntity.builder()
                .setFileId(context.getUploadSession().getFileId())
                .setBlobId(blobEntity.getBlobId())
                .setGroupName(context.getUploadSession().getGroupName())
                .setOwnerUserId(context.getUploadSession().getOwnerUserId())
                .setFileName(context.getUploadSession().getFileName())
                .setFileSize(context.getPreparedBlob().getSize())
                .setMimeType(context.getPreparedBlob().getMimeType())
                .setFileType(context.getPreparedBlob().getFileType())
                .setVisibility(groupConfig.getVisibility())
                .setCreateTime(now)
                .setUpdateTime(now)
                .build();
        StorageFileEntity savedFileEntity = storageFileRepository.save(fileEntity);
        context.getUploadSession().markCompleted(now);
        storageUploadSessionRepository.save(StorageUploadSessionEntity.toEntity(context.getUploadSession()));
        return new PersistedMaterialization(toFileStorage(savedFileEntity), blobEntity.getBlobId());
    }

    private String newBlobId() {
        return resourceIdGenerator.nextId(StorageResourceKind.INSTANCE);
    }

    private FileStorage toFileStorage(StorageFileEntity fileEntity) {
        return new FileStorage(
                fileEntity.getFileId(),
                fileEntity.getFileName(),
                fileEntity.getFileSize(),
                fileEntity.getMimeType(),
                fileEntity.getFileType(),
                fileEntity.getCreateTime()
        );
    }
}
