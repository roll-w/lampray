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

package tech.lamprism.lampray.storage.facade;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tech.lamprism.lampray.common.data.ResourceIdGenerator;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageResourceKind;
import tech.lamprism.lampray.storage.configuration.StorageTopology;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.materialization.StorageBlobMaterializationService;
import tech.lamprism.lampray.storage.materialization.StorageMaterializationContext;
import tech.lamprism.lampray.storage.materialization.StorageMaterializationHook;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.session.StorageUploadSessionService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class DefaultStorageFilePersistenceService implements StorageFilePersistenceService {
    private final StorageTopology storageTopology;
    private final StorageBlobMaterializationService storageBlobMaterializationService;
    private final StorageFileRepository storageFileRepository;
    private final StorageUploadSessionService storageUploadSessionService;
    private final ResourceIdGenerator resourceIdGenerator;
    private final List<StorageMaterializationHook> storageMaterializationHooks;
    private final TransactionTemplate transactionTemplate;

    public DefaultStorageFilePersistenceService(StorageTopology storageTopology,
                                                StorageBlobMaterializationService storageBlobMaterializationService,
                                                StorageFileRepository storageFileRepository,
                                                StorageUploadSessionService storageUploadSessionService,
                                                ResourceIdGenerator resourceIdGenerator,
                                                List<StorageMaterializationHook> storageMaterializationHooks,
                                                PlatformTransactionManager transactionManager) {
        this.storageTopology = storageTopology;
        this.storageBlobMaterializationService = storageBlobMaterializationService;
        this.storageFileRepository = storageFileRepository;
        this.storageUploadSessionService = storageUploadSessionService;
        this.resourceIdGenerator = resourceIdGenerator;
        this.storageMaterializationHooks = List.copyOf(storageMaterializationHooks);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public FileStorage persistSessionUpload(StorageUploadSessionEntity uploadSession,
                                            PreparedBlobMaterialization preparedBlob) {
        PersistedMaterialization persistedMaterialization = Objects.requireNonNull(transactionTemplate.execute(status -> {
            StorageBlobEntity blobEntity = storageBlobMaterializationService.persistBlobMaterialization(preparedBlob);
            return createSessionFileEntity(uploadSession, blobEntity, preparedBlob.size());
        }));
        notifyMaterializationHooks(
                persistedMaterialization.fileStorage(),
                persistedMaterialization.blobId(),
                uploadSession.getGroupName(),
                uploadSession.getOwnerUserId()
        );
        return persistedMaterialization.fileStorage();
    }

    @Override
    public FileStorage persistTrustedUpload(String groupName,
                                            String fileName,
                                            String mimeType,
                                            FileType fileType,
                                            Long ownerUserId,
                                            PreparedBlobMaterialization preparedBlob) {
        PersistedMaterialization persistedMaterialization = Objects.requireNonNull(transactionTemplate.execute(status -> {
            StorageBlobEntity blobEntity = storageBlobMaterializationService.persistBlobMaterialization(preparedBlob);
            return createTrustedFileEntity(groupName, fileName, mimeType, fileType, ownerUserId, blobEntity, preparedBlob.size());
        }));
        notifyMaterializationHooks(
                persistedMaterialization.fileStorage(),
                persistedMaterialization.blobId(),
                groupName,
                ownerUserId
        );
        return persistedMaterialization.fileStorage();
    }

    private PersistedMaterialization createSessionFileEntity(StorageUploadSessionEntity uploadSession,
                                                             StorageBlobEntity blobEntity,
                                                             long size) {
        OffsetDateTime now = OffsetDateTime.now();
        StorageFileEntity fileEntity = StorageFileEntity.builder()
                .setFileId(uploadSession.getFileId())
                .setBlobId(blobEntity.getBlobId())
                .setGroupName(uploadSession.getGroupName())
                .setOwnerUserId(uploadSession.getOwnerUserId())
                .setFileName(uploadSession.getFileName())
                .setFileSize(size)
                .setMimeType(uploadSession.getMimeType())
                .setFileType(uploadSession.getFileType())
                .setVisibility(storageTopology.getGroup(uploadSession.getGroupName()).getVisibility())
                .setCreateTime(now)
                .setUpdateTime(now)
                .build();
        StorageFileEntity savedFileEntity = storageFileRepository.save(fileEntity);
        storageUploadSessionService.markCompleted(uploadSession, now);
        return new PersistedMaterialization(savedFileEntity.lock(), blobEntity.getBlobId());
    }

    private PersistedMaterialization createTrustedFileEntity(String groupName,
                                                             String fileName,
                                                             String mimeType,
                                                             FileType fileType,
                                                             Long ownerUserId,
                                                             StorageBlobEntity blobEntity,
                                                             long size) {
        OffsetDateTime now = OffsetDateTime.now();
        StorageFileEntity fileEntity = StorageFileEntity.builder()
                .setFileId(newId())
                .setBlobId(blobEntity.getBlobId())
                .setGroupName(groupName)
                .setOwnerUserId(ownerUserId)
                .setFileName(fileName)
                .setFileSize(size)
                .setMimeType(mimeType)
                .setFileType(fileType)
                .setVisibility(storageTopology.getGroup(groupName).getVisibility())
                .setCreateTime(now)
                .setUpdateTime(now)
                .build();
        StorageFileEntity savedFileEntity = storageFileRepository.save(fileEntity);
        return new PersistedMaterialization(savedFileEntity.lock(), blobEntity.getBlobId());
    }

    private void notifyMaterializationHooks(FileStorage fileStorage,
                                            String blobId,
                                            String groupName,
                                            Long ownerUserId) {
        StorageMaterializationContext context = new StorageMaterializationContext(fileStorage, blobId, groupName, ownerUserId);
        for (StorageMaterializationHook storageMaterializationHook : storageMaterializationHooks) {
            storageMaterializationHook.onMaterialized(context);
        }
    }

    private String newId() {
        return resourceIdGenerator.nextId(StorageResourceKind.INSTANCE);
    }
}
