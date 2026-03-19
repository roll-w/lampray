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

package tech.lamprism.lampray.storage.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageException;
import tech.rollw.common.web.CommonErrorCode;

import java.util.Locale;

/**
 * Centralizes storage content normalization and delivery safety rules.
 *
 * @author RollW
 */
@Component
public class DefaultStorageContentPolicy implements StorageContentPolicy {
    @Override
    public String requireMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "MIME type is required.");
        }
        return normalizeMimeType(mimeType);
    }

    @Override
    public String normalizeMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return "application/octet-stream";
        }
        String normalizedMimeType = mimeType.trim().toLowerCase(Locale.ROOT);
        int parameterIndex = normalizedMimeType.indexOf(';');
        if (parameterIndex < 0) {
            return normalizedMimeType;
        }
        String baseMimeType = normalizedMimeType.substring(0, parameterIndex).trim();
        if (baseMimeType.isEmpty()) {
            return "application/octet-stream";
        }
        String parameters = normalizedMimeType.substring(parameterIndex + 1).trim();
        return parameters.isEmpty() ? baseMimeType : baseMimeType + "; " + parameters;
    }

    @Override
    public FileType resolveFileType(String mimeType) {
        return FileType.fromMimeType(baseMimeType(mimeType));
    }

    @Override
    public boolean isUnsafeDirectMimeType(String mimeType) {
        String normalizedMimeType = baseMimeType(mimeType);
        return "text/html".equals(normalizedMimeType)
                || "application/xhtml+xml".equals(normalizedMimeType)
                || "image/svg+xml".equals(normalizedMimeType)
                || "text/xml".equals(normalizedMimeType)
                || "application/xml".equals(normalizedMimeType)
                || normalizedMimeType.endsWith("+xml")
                || "text/javascript".equals(normalizedMimeType)
                || "application/javascript".equals(normalizedMimeType)
                || "application/json".equals(normalizedMimeType);
    }

    private String baseMimeType(String mimeType) {
        String normalizedMimeType = normalizeMimeType(mimeType);
        int parameterIndex = normalizedMimeType.indexOf(';');
        if (parameterIndex >= 0) {
            normalizedMimeType = normalizedMimeType.substring(0, parameterIndex).trim();
        }
        return normalizedMimeType;
    }
}
