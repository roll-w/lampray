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

package tech.lamprism.lampray.storage.support;

import java.util.Map;

/**
 * @author RollW
 */
public final class BlobMetadataSupport {
    public static final String CONTENT_CHECKSUM_KEY = "checksum-sha256";

    private BlobMetadataSupport() {
    }

    public static Map<String, String> contentChecksumMetadata(String contentChecksum) {
        if (contentChecksum == null) {
            return Map.of();
        }
        return Map.of(CONTENT_CHECKSUM_KEY, contentChecksum);
    }

    public static String metadataContentChecksum(Map<String, String> metadata) {
        return metadata.get(CONTENT_CHECKSUM_KEY);
    }
}
