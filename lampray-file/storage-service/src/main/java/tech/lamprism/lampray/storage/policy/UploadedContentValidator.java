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
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.materialization.TempUpload;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.rollw.common.web.CommonErrorCode;

@Component
public class UploadedContentValidator {
    private final ChecksumNormalizer checksumNormalizer;
    private final ChecksumValidator checksumValidator;

    public UploadedContentValidator(ChecksumNormalizer checksumNormalizer,
                                    ChecksumValidator checksumValidator) {
        this.checksumNormalizer = checksumNormalizer;
        this.checksumValidator = checksumValidator;
    }

    public void validate(StorageUploadSessionEntity uploadSession,
                         TempUpload tempUpload,
                         StorageGroupConfig groupSettings) {
        validateGroupSizeLimit(groupSettings.getMaxSizeBytes(), tempUpload.getSize());
        validateDeclaredSize(uploadSession.getFileSize(), tempUpload.getSize());
        String expectedChecksum = checksumNormalizer.normalize(uploadSession.getChecksumSha256());
        if (expectedChecksum != null) {
            checksumValidator.validateMatch(expectedChecksum, tempUpload.getChecksumSha256());
        }
    }

    private void validateGroupSizeLimit(Long maxSizeBytes,
                                         long actualSize) {
        if (maxSizeBytes != null && actualSize > maxSizeBytes) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Uploaded file size exceeds the configured group limit.");
        }
    }

    private void validateDeclaredSize(Long declaredSize,
                                      long actualSize) {
        if (declaredSize != null && declaredSize != actualSize) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Uploaded file size does not match declared size.");
        }
    }
}
