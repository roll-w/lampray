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

import tech.lamprism.lampray.storage.StorageAccessRequest;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.StorageUploadSession;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
public record StorageUploadSessionResponse(
        String uploadId,
        StorageUploadMode mode,
        String fileName,
        String groupName,
        String fileId,
        StorageAccessRequest directRequest,
        OffsetDateTime expiresAt
) {
    public static StorageUploadSessionResponse from(StorageUploadSession uploadSession) {
        return new StorageUploadSessionResponse(
                uploadSession.getUploadId(),
                uploadSession.getMode(),
                uploadSession.getFileName(),
                uploadSession.getGroupName(),
                uploadSession.getFileId(),
                uploadSession.getDirectRequest(),
                uploadSession.getExpiresAt()
        );
    }
}
