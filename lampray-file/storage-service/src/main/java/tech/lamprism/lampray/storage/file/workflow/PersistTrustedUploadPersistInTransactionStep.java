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
import tech.lamprism.lampray.storage.materialization.persistence.BlobMaterializationPersistenceService;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class PersistTrustedUploadPersistInTransactionStep implements WorkflowStep<PersistTrustedUploadWorkflowContext> {
    private static final int STEP_ORDER = 100;

    private final BlobMaterializationPersistenceService blobMaterializationPersistenceService;
    private final StorageFileRepository storageFileRepository;
    private final StorageTopology storageTopology;
    private final ResourceIdGenerator resourceIdGenerator;
    private final TransactionTemplate transactionTemplate;

    PersistTrustedUploadPersistInTransactionStep(BlobMaterializationPersistenceService blobMaterializationPersistenceService,
                                                 StorageFileRepository storageFileRepository,
                                                 StorageTopology storageTopology,
                                                 ResourceIdGenerator resourceIdGenerator,
                                                 PlatformTransactionManager transactionManager) {
        this.blobMaterializationPersistenceService = blobMaterializationPersistenceService;
        this.storageFileRepository = storageFileRepository;
        this.storageTopology = storageTopology;
        this.resourceIdGenerator = resourceIdGenerator;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public int getOrder() {
        return STEP_ORDER;
    }

    @Override
    public void execute(PersistTrustedUploadWorkflowContext context) {
        PersistedMaterialization persistedMaterialization = Objects.requireNonNull(
                transactionTemplate.execute(status -> persistTrustedUpload(context)),
                "persistedMaterialization"
        );
        context.getState().setPersistedMaterialization(persistedMaterialization);
        context.getState().setResult(persistedMaterialization.getFileStorage());
    }

    private PersistedMaterialization persistTrustedUpload(PersistTrustedUploadWorkflowContext context) {
        StorageBlobEntity blobEntity = blobMaterializationPersistenceService.persist(context.getPreparedBlob());
        OffsetDateTime now = OffsetDateTime.now();
        StorageGroupConfig groupConfig = storageTopology.getGroup(context.getGroupName());
        StorageFileEntity fileEntity = StorageFileEntity.builder()
                .setFileId(newFileId())
                .setBlobId(blobEntity.getBlobId())
                .setGroupName(context.getGroupName())
                .setOwnerUserId(context.getOwnerUserId())
                .setFileName(context.getFileName())
                .setFileSize(context.getPreparedBlob().getSize())
                .setMimeType(context.getMimeType())
                .setFileType(context.getFileType())
                .setVisibility(groupConfig.getVisibility())
                .setCreateTime(now)
                .setUpdateTime(now)
                .build();
        StorageFileEntity savedFileEntity = storageFileRepository.save(fileEntity);
        return new PersistedMaterialization(toFileStorage(savedFileEntity), blobEntity.getBlobId());
    }

    private String newFileId() {
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
