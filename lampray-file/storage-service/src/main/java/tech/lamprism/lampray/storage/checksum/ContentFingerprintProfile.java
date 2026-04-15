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

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.util.Objects;

/**
 * Defines how dual content fingerprints are encoded and calculated.
 *
 * @author RollW
 */
public final class ContentFingerprintProfile {
    public static final char VALUE_SEPARATOR = '$';
    public static final ContentFingerprintProfile DEFAULT = new ContentFingerprintProfile(
            "sha256+murmur3-128",
            "content-fingerprint",
            "checksum-sha256",
            "Primary SHA-256 fingerprint",
            64,
            Hashing.sha256(),
            "Secondary Murmur3-128 fingerprint",
            32,
            Hashing.murmur3_128()
    );

    private final String encodingId;
    private final String fingerprintMetadataKey;
    private final String primaryChecksumMetadataKey;
    private final String primaryLabel;
    private final int primaryHexLength;
    private final HashFunction primaryHashFunction;
    private final String secondaryLabel;
    private final int secondaryHexLength;
    private final HashFunction secondaryHashFunction;

    public ContentFingerprintProfile(String encodingId,
                                     String fingerprintMetadataKey,
                                     String primaryChecksumMetadataKey,
                                     String primaryLabel,
                                     int primaryHexLength,
                                     HashFunction primaryHashFunction,
                                     String secondaryLabel,
                                     int secondaryHexLength,
                                     HashFunction secondaryHashFunction) {
        this.encodingId = requireText(encodingId, "encodingId");
        this.fingerprintMetadataKey = requireText(fingerprintMetadataKey, "fingerprintMetadataKey");
        this.primaryChecksumMetadataKey = requireText(primaryChecksumMetadataKey, "primaryChecksumMetadataKey");
        this.primaryLabel = requireText(primaryLabel, "primaryLabel");
        this.primaryHexLength = requirePositive(primaryHexLength, "primaryHexLength");
        this.primaryHashFunction = Objects.requireNonNull(primaryHashFunction, "primaryHashFunction must not be null");
        this.secondaryLabel = requireText(secondaryLabel, "secondaryLabel");
        this.secondaryHexLength = requirePositive(secondaryHexLength, "secondaryHexLength");
        this.secondaryHashFunction = Objects.requireNonNull(secondaryHashFunction, "secondaryHashFunction must not be null");
    }

    public static ContentFingerprintProfile defaultProfile() {
        return DEFAULT;
    }

    public String encodingId() {
        return encodingId;
    }

    public String encodedPrefix() {
        return "{" + encodingId + "}";
    }

    public String encodedFormatDescription() {
        return encodedPrefix() + "<primary>$<secondary>";
    }

    public String fingerprintMetadataKey() {
        return fingerprintMetadataKey;
    }

    public String primaryChecksumMetadataKey() {
        return primaryChecksumMetadataKey;
    }

    public String primaryLabel() {
        return primaryLabel;
    }

    public int primaryHexLength() {
        return primaryHexLength;
    }

    public HashFunction primaryHashFunction() {
        return primaryHashFunction;
    }

    public String secondaryLabel() {
        return secondaryLabel;
    }

    public int secondaryHexLength() {
        return secondaryHexLength;
    }

    public HashFunction secondaryHashFunction() {
        return secondaryHashFunction;
    }

    private static String requireText(String value,
                                      String label) {
        String normalized = Objects.requireNonNull(value, label + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank.");
        }
        return normalized;
    }

    private static int requirePositive(int value,
                                       String label) {
        if (value <= 0) {
            throw new IllegalArgumentException(label + " must be positive.");
        }
        return value;
    }
}
