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

package tech.lamprism.lampray.web.controller.storage.model;

import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.StorageUploadSessionDetails;
import tech.lamprism.lampray.storage.StorageUploadSessionState;

import java.time.OffsetDateTime;

public record StorageUploadSessionDetailsResponse(
        String uploadId,
        StorageUploadMode mode,
        String fileName,
        String groupName,
        String fileId,
        StorageUploadSessionState state,
        OffsetDateTime expiresAt,
        OffsetDateTime createTime,
        OffsetDateTime updateTime,
        StorageFileSummaryResponse fileStorage
) {
    public static StorageUploadSessionDetailsResponse from(StorageUploadSessionDetails uploadSession) {
        return new StorageUploadSessionDetailsResponse(
                uploadSession.getUploadId(),
                uploadSession.getMode(),
                uploadSession.getFileName(),
                uploadSession.getGroupName(),
                uploadSession.getFileId(),
                uploadSession.getState(),
                uploadSession.getExpiresAt(),
                uploadSession.getCreateTime(),
                uploadSession.getUpdateTime(),
                uploadSession.getFileStorage() == null ? null : StorageFileSummaryResponse.from(uploadSession.getFileStorage())
        );
    }
}
