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

package tech.lamprism.lampray.storage.session;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.UploadSessionStatus;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
@Service
public class StorageUploadSessionEntityFactory {
    public StorageUploadSessionEntity createPendingSession(String uploadId,
                                                           String fileId,
                                                           String groupName,
                                                           String fileName,
                                                           Long fileSize,
                                                           String mimeType,
                                                           FileType fileType,
                                                           String checksum,
                                                           Long ownerUserId,
                                                           String primaryBackend,
                                                           String objectKey,
                                                           StorageUploadMode uploadMode,
                                                           OffsetDateTime expiresAt,
                                                           OffsetDateTime now) {
        return StorageUploadSessionEntity.builder()
                .setUploadId(uploadId)
                .setFileId(fileId)
                .setGroupName(groupName)
                .setFileName(fileName)
                .setFileSize(fileSize)
                .setMimeType(mimeType)
                .setFileType(fileType)
                .setChecksumSha256(checksum)
                .setOwnerUserId(ownerUserId)
                .setPrimaryBackend(primaryBackend)
                .setObjectKey(objectKey)
                .setUploadMode(uploadMode)
                .setStatus(UploadSessionStatus.PENDING)
                .setExpiresAt(expiresAt)
                .setCreateTime(now)
                .setUpdateTime(now)
                .build();
    }
}
