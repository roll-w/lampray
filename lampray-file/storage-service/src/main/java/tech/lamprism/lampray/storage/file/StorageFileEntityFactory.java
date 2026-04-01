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
import tech.lamprism.lampray.common.data.ResourceIdGenerator;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageResourceKind;
import tech.lamprism.lampray.storage.configuration.StorageTopology;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.domain.StorageUploadSessionModel;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
@Service
public class StorageFileEntityFactory {
    private final StorageTopology storageTopology;
    private final ResourceIdGenerator resourceIdGenerator;

    public StorageFileEntityFactory(StorageTopology storageTopology,
                                    ResourceIdGenerator resourceIdGenerator) {
        this.storageTopology = storageTopology;
        this.resourceIdGenerator = resourceIdGenerator;
    }

    public StorageFileEntity createSessionFile(StorageUploadSessionModel uploadSession,
                                               String blobId,
                                               PreparedBlobMaterialization preparedBlob,
                                               OffsetDateTime now) {
        return buildFileEntity(
                uploadSession.getFileId(),
                blobId,
                uploadSession.getGroupName(),
                uploadSession.getOwnerUserId(),
                uploadSession.getFileName(),
                preparedBlob.getSize(),
                preparedBlob.getMimeType(),
                preparedBlob.getFileType(),
                now
        );
    }

    public StorageFileEntity createTrustedFile(String groupName,
                                               String fileName,
                                               String mimeType,
                                               FileType fileType,
                                               Long ownerUserId,
                                               String blobId,
                                               long size,
                                               OffsetDateTime now) {
        return buildFileEntity(
                newId(),
                blobId,
                groupName,
                ownerUserId,
                fileName,
                size,
                mimeType,
                fileType,
                now
        );
    }

    private StorageFileEntity buildFileEntity(String fileId,
                                              String blobId,
                                              String groupName,
                                              Long ownerUserId,
                                              String fileName,
                                              long size,
                                              String mimeType,
                                              FileType fileType,
                                              OffsetDateTime now) {
        return new StorageFileEntity(
                null,
                fileId,
                blobId,
                groupName,
                ownerUserId,
                fileName,
                size,
                mimeType,
                fileType,
                storageTopology.getGroup(groupName).getVisibility(),
                now,
                now
        );
    }

    private String newId() {
        return resourceIdGenerator.nextId(StorageResourceKind.INSTANCE);
    }
}
