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

import java.util.Locale;
import java.util.Objects;

/**
 * Encodes a dual fingerprint as a single checksum string.
 *
 * @author RollW
 */
public final class ContentFingerprint {
    private final ContentFingerprintProfile profile;
    private final String primaryChecksum;
    private final String secondaryChecksum;

    private ContentFingerprint(ContentFingerprintProfile profile,
                               String primaryChecksum,
                               String secondaryChecksum) {
        this.profile = Objects.requireNonNull(profile, "profile must not be null");
        this.primaryChecksum = requireHex(primaryChecksum, profile.primaryHexLength(), profile.primaryLabel());
        this.secondaryChecksum = requireHex(secondaryChecksum, profile.secondaryHexLength(), profile.secondaryLabel());
    }

    public static ContentFingerprint of(String primaryChecksum,
                                        String secondaryChecksum) {
        return of(ContentFingerprintProfile.defaultProfile(), primaryChecksum, secondaryChecksum);
    }

    public static ContentFingerprint of(ContentFingerprintProfile profile,
                                        String primaryChecksum,
                                        String secondaryChecksum) {
        return new ContentFingerprint(profile, primaryChecksum, secondaryChecksum);
    }

    public static ContentFingerprint parse(String rawValue) {
        return parse(rawValue, ContentFingerprintProfile.defaultProfile());
    }

    public static ContentFingerprint parse(String rawValue,
                                           ContentFingerprintProfile profile) {
        ContentFingerprintProfile fingerprintProfile = Objects.requireNonNull(profile, "profile must not be null");
        String normalized = Objects.requireNonNull(rawValue, "contentFingerprint must not be null")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!normalized.startsWith(fingerprintProfile.encodedPrefix())) {
            throw new IllegalArgumentException(
                    "Checksum must use the format " + fingerprintProfile.encodedFormatDescription() + "."
            );
        }
        String payload = normalized.substring(fingerprintProfile.encodedPrefix().length());
        int separatorIndex = payload.indexOf(ContentFingerprintProfile.VALUE_SEPARATOR);
        if (separatorIndex <= 0
                || separatorIndex >= payload.length() - 1
                || payload.indexOf(ContentFingerprintProfile.VALUE_SEPARATOR, separatorIndex + 1) >= 0) {
            throw new IllegalArgumentException(
                    "Checksum must use the format " + fingerprintProfile.encodedFormatDescription() + "."
            );
        }
        String primaryChecksum = payload.substring(0, separatorIndex);
        String secondaryChecksum = payload.substring(separatorIndex + 1);
        return of(fingerprintProfile, primaryChecksum, secondaryChecksum);
    }

    public static String normalizePrimaryChecksum(String rawValue) {
        return normalizePrimaryChecksum(rawValue, ContentFingerprintProfile.defaultProfile());
    }

    public static String normalizePrimaryChecksum(String rawValue,
                                                  ContentFingerprintProfile profile) {
        ContentFingerprintProfile fingerprintProfile = Objects.requireNonNull(profile, "profile must not be null");
        return requireHex(rawValue, fingerprintProfile.primaryHexLength(), fingerprintProfile.primaryLabel());
    }

    public ContentFingerprintProfile profile() {
        return profile;
    }

    public String primaryChecksum() {
        return primaryChecksum;
    }

    public String secondaryFingerprint() {
        return secondaryChecksum;
    }

    public String encoded() {
        return profile.encodedPrefix() + primaryChecksum + ContentFingerprintProfile.VALUE_SEPARATOR + secondaryChecksum;
    }

    @Override
    public String toString() {
        return encoded();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ContentFingerprint)) {
            return false;
        }
        ContentFingerprint that = (ContentFingerprint) other;
        return profile.encodingId().equals(that.profile.encodingId())
                && primaryChecksum.equals(that.primaryChecksum)
                && secondaryChecksum.equals(that.secondaryChecksum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profile.encodingId(), primaryChecksum, secondaryChecksum);
    }

    private static String requireHex(String rawValue,
                                     int expectedLength,
                                     String label) {
        String normalized = Objects.requireNonNull(rawValue, label + " must not be null")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.length() != expectedLength) {
            throw new IllegalArgumentException(label + " must be a " + expectedLength + "-character lowercase hex string.");
        }
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            boolean numeric = current >= '0' && current <= '9';
            boolean alphabetic = current >= 'a' && current <= 'f';
            if (!numeric && !alphabetic) {
                throw new IllegalArgumentException(label + " must be a " + expectedLength + "-character lowercase hex string.");
            }
        }
        return normalized;
    }
}
