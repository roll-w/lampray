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

import org.apache.commons.lang3.StringUtils;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.materialization.TempUpload;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.store.BlobObject;
import tech.rollw.common.web.CommonErrorCode;

import java.util.Locale;

/**
 * @author RollW
 */
public final class StorageValidationRules {
    public static final StorageValidationRules INSTANCE = new StorageValidationRules();

    private StorageValidationRules() {
    }

    public String normalizeFileName(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "File name is required.");
        }
        String normalized = fileName.trim();
        int slashIndex = normalized.lastIndexOf('/');
        int backslashIndex = normalized.lastIndexOf('\\');
        int separatorIndex = Math.max(slashIndex, backslashIndex);
        if (separatorIndex >= 0 && separatorIndex < normalized.length() - 1) {
            normalized = normalized.substring(separatorIndex + 1);
        }
        if (normalized.isBlank() || ".".equals(normalized) || "..".equals(normalized)) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "File name is invalid.");
        }
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            if (current == '\r' || current == '\n') {
                throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "File name is invalid.");
            }
        }
        return normalized;
    }

    public String normalizeChecksum(String checksumSha256) {
        if (StringUtils.isBlank(checksumSha256)) {
            return null;
        }
        String normalized = checksumSha256.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() != 64) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Checksum must be a 64-character SHA-256 hex string.");
        }
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            boolean numeric = current >= '0' && current <= '9';
            boolean alphabetic = current >= 'a' && current <= 'f';
            if (!numeric && !alphabetic) {
                throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                        "Checksum must be a lowercase SHA-256 hex string.");
            }
        }
        return normalized;
    }

    public void validateChecksumMatch(String expectedChecksum,
                                      String actualChecksum) {
        if (!expectedChecksum.equals(actualChecksum)) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Uploaded file checksum does not match declared checksum.");
        }
    }

    public void validateUploadRequest(StorageUploadRequest request,
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

    public void validateUploadedContent(StorageUploadSessionEntity uploadSession,
                                        TempUpload tempUpload,
                                        StorageGroupConfig groupSettings) {
        validateGroupSizeLimit(groupSettings.getMaxSizeBytes(), tempUpload.getSize());
        validateDeclaredSize(uploadSession.getFileSize(), tempUpload.getSize());
        String expectedChecksum = normalizeChecksum(uploadSession.getChecksumSha256());
        if (expectedChecksum != null) {
            validateChecksumMatch(expectedChecksum, tempUpload.getChecksumSha256());
        }
    }

    public void validateUploadedObject(StorageUploadSessionEntity uploadSession,
                                       BlobObject uploadedObject,
                                       StorageGroupConfig groupSettings) {
        validateGroupSizeLimit(groupSettings.getMaxSizeBytes(), uploadedObject.getSize());
        validateDeclaredSize(uploadSession.getFileSize(), uploadedObject.getSize());
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
