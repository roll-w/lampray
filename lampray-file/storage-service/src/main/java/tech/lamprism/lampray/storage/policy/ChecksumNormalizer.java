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

import java.util.Locale;

@Component
public class ChecksumNormalizer {
    public String normalize(String checksumSha256) {
        if (!StringUtils.hasText(checksumSha256)) {
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
}
