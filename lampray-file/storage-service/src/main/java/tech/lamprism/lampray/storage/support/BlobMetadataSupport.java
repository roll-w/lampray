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

import tech.lamprism.lampray.storage.checksum.ContentFingerprint;
import tech.lamprism.lampray.storage.checksum.ContentFingerprintProfile;

import java.util.Map;

/**
 * @author RollW
 */
public final class BlobMetadataSupport {
    public static final String CONTENT_FINGERPRINT_KEY = ContentFingerprintProfile.defaultProfile().fingerprintMetadataKey();
    public static final String CONTENT_CHECKSUM_KEY = ContentFingerprintProfile.defaultProfile().primaryChecksumMetadataKey();

    private BlobMetadataSupport() {
    }

    public static Map<String, String> contentFingerprintMetadata(String contentChecksum) {
        return contentFingerprintMetadata(contentChecksum, ContentFingerprintProfile.defaultProfile());
    }

    public static Map<String, String> contentFingerprintMetadata(String contentChecksum,
                                                                 ContentFingerprintProfile profile) {
        if (contentChecksum == null) {
            return Map.of();
        }
        ContentFingerprint contentFingerprint = ContentFingerprint.parse(contentChecksum, profile);
        return Map.of(
                profile.fingerprintMetadataKey(), contentFingerprint.encoded(),
                profile.primaryChecksumMetadataKey(), contentFingerprint.primaryChecksum()
        );
    }

    public static String metadataContentFingerprint(Map<String, String> metadata) {
        return metadataContentFingerprint(metadata, ContentFingerprintProfile.defaultProfile());
    }

    public static String metadataContentFingerprint(Map<String, String> metadata,
                                                    ContentFingerprintProfile profile) {
        return metadata.get(profile.fingerprintMetadataKey());
    }

    public static String metadataContentChecksum(Map<String, String> metadata) {
        return metadataContentChecksum(metadata, ContentFingerprintProfile.defaultProfile());
    }

    public static String metadataContentChecksum(Map<String, String> metadata,
                                                 ContentFingerprintProfile profile) {
        return metadata.get(profile.primaryChecksumMetadataKey());
    }
}
