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

package tech.lamprism.lampray.storage.policy;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.rollw.common.web.CommonErrorCode;

@Component
public class UploadRequestValidator {
    public void validate(StorageUploadRequest request,
                         StorageGroupConfig groupSettings,
                         FileType fileType) {
        Long requestedSize = request.getSize();
        if (requestedSize != null && requestedSize < 0) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "File size cannot be negative.");
        }
        if (requestedSize != null && groupSettings.getMaxSizeBytes() != null
                && requestedSize > groupSettings.getMaxSizeBytes()) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Uploaded file size exceeds the configured group limit.");
        }
        if (!groupSettings.getAllowedFileTypes().isEmpty() && !groupSettings.getAllowedFileTypes().contains(fileType)) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "File type is not allowed for this storage group.");
        }
    }
}
