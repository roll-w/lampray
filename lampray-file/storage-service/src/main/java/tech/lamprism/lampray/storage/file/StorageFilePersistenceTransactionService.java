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
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageTopology;
import tech.lamprism.lampray.storage.domain.StorageUploadSessionModel;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.materialization.persistence.BlobMaterializationPersistenceService;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * @author RollW
 */
@Service
public class StorageFilePersistenceTransactionService {
    private final BlobMaterializationPersistenceService blobMaterializationPersistenceService;
    private final StorageFileEntityFactory storageFileEntityFactory;
    private final StorageFileRepository storageFileRepository;
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final StorageTopology storageTopology;
    private final TransactionTemplate transactionTemplate;

    public StorageFilePersistenceTransactionService(BlobMaterializationPersistenceService blobMaterializationPersistenceService,
                                                    StorageFileEntityFactory storageFileEntityFactory,
                                                    StorageFileRepository storageFileRepository,
                                                    StorageUploadSessionRepository storageUploadSessionRepository,
                                                    StorageTopology storageTopology,
                                                    PlatformTransactionManager transactionManager) {
        this.blobMaterializationPersistenceService = blobMaterializationPersistenceService;
        this.storageFileEntityFactory = storageFileEntityFactory;
        this.storageFileRepository = storageFileRepository;
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.storageTopology = storageTopology;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public PersistedMaterialization persistSessionUpload(StorageUploadSessionModel uploadSession,
                                                         PreparedBlobMaterialization preparedBlob) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            StorageBlobEntity blobEntity = blobMaterializationPersistenceService.persist(preparedBlob);
            OffsetDateTime now = OffsetDateTime.now();
            StorageGroupConfig groupConfig = storageTopology.getGroup(uploadSession.getGroupName());
            StorageFileEntity fileEntity = storageFileEntityFactory.createSessionFile(
                    uploadSession,
                    blobEntity.getBlobId(),
                    preparedBlob,
                    groupConfig.getVisibility(),
                    now
            );
            StorageFileEntity savedFileEntity = storageFileRepository.save(fileEntity);
            uploadSession.markCompleted(now);
            storageUploadSessionRepository.save(uploadSession.getEntity());
            return new PersistedMaterialization(toFileStorage(savedFileEntity), blobEntity.getBlobId());
        }));
    }

    public PersistedMaterialization persistTrustedUpload(String groupName,
                                                         String fileName,
                                                         String mimeType,
                                                         FileType fileType,
                                                         Long ownerUserId,
                                                         PreparedBlobMaterialization preparedBlob) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            StorageBlobEntity blobEntity = blobMaterializationPersistenceService.persist(preparedBlob);
            OffsetDateTime now = OffsetDateTime.now();
            StorageGroupConfig groupConfig = storageTopology.getGroup(groupName);
            StorageFileEntity fileEntity = storageFileEntityFactory.createTrustedFile(
                    groupName,
                    fileName,
                    mimeType,
                    fileType,
                    ownerUserId,
                    blobEntity.getBlobId(),
                    preparedBlob.getSize(),
                    groupConfig.getVisibility(),
                    now
            );
            StorageFileEntity savedFileEntity = storageFileRepository.save(fileEntity);
            return new PersistedMaterialization(toFileStorage(savedFileEntity), blobEntity.getBlobId());
        }));
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
