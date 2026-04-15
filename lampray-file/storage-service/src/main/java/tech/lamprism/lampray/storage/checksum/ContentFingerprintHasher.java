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

package tech.lamprism.lampray.storage.checksum;

import com.google.common.hash.Hasher;

import java.util.Objects;

/**
 * Streams bytes into the configured dual fingerprint.
 *
 * @author RollW
 */
public final class ContentFingerprintHasher {
    private final ContentFingerprintProfile profile;
    private final Hasher primaryHasher;
    private final Hasher secondaryHasher;
    private final String primaryChecksum;

    private ContentFingerprintHasher(ContentFingerprintProfile profile,
                                     String primaryChecksum) {
        this.profile = Objects.requireNonNull(profile, "profile must not be null");
        this.primaryChecksum = primaryChecksum != null
                ? ContentFingerprint.normalizePrimaryChecksum(primaryChecksum, profile)
                : null;
        this.primaryHasher = this.primaryChecksum == null ? profile.primaryHashFunction().newHasher() : null;
        this.secondaryHasher = profile.secondaryHashFunction().newHasher();
    }

    public static ContentFingerprintHasher create() {
        return create(ContentFingerprintProfile.defaultProfile());
    }

    public static ContentFingerprintHasher create(ContentFingerprintProfile profile) {
        return new ContentFingerprintHasher(profile, null);
    }

    public static ContentFingerprintHasher forKnownPrimaryChecksum(String primaryChecksum) {
        return forKnownPrimaryChecksum(primaryChecksum, ContentFingerprintProfile.defaultProfile());
    }

    public static ContentFingerprintHasher forKnownPrimaryChecksum(String primaryChecksum,
                                                                  ContentFingerprintProfile profile) {
        return new ContentFingerprintHasher(profile, primaryChecksum);
    }

    public void putBytes(byte[] buffer,
                         int offset,
                         int length) {
        if (primaryHasher != null) {
            primaryHasher.putBytes(buffer, offset, length);
        }
        secondaryHasher.putBytes(buffer, offset, length);
    }

    public ContentFingerprint finish() {
        String resolvedPrimaryChecksum = primaryChecksum != null ? primaryChecksum : primaryHasher.hash().toString();
        return ContentFingerprint.of(profile, resolvedPrimaryChecksum, secondaryHasher.hash().toString());
    }
}
