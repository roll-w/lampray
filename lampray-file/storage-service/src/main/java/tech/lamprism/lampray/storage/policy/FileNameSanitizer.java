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
import org.springframework.util.StringUtils;
import tech.lamprism.lampray.storage.StorageException;
import tech.rollw.common.web.CommonErrorCode;

@Component
public class FileNameSanitizer {
    public String normalize(String fileName) {
        if (!StringUtils.hasText(fileName)) {
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
}
