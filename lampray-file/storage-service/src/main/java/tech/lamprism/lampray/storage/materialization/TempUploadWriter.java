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

package tech.lamprism.lampray.storage.materialization;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.StorageException;
import tech.rollw.common.web.CommonErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class TempUploadWriter {
    private static final int BUFFER_SIZE = 8192;

    public TempUpload write(InputStream inputStream,
                            Long maxSizeBytes) throws IOException {
        Path tempFile = Files.createTempFile("lampray-upload-", ".bin");
        MessageDigest digest = newSha256Digest();
        long size = 0;
        try {
            try (InputStream source = inputStream;
                 var outputStream = Files.newOutputStream(tempFile, StandardOpenOption.TRUNCATE_EXISTING)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = source.read(buffer)) != -1) {
                    size += read;
                    if (maxSizeBytes != null && size > maxSizeBytes) {
                        throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                                "Uploaded file size exceeds the configured group limit.");
                    }
                    digest.update(buffer, 0, read);
                    outputStream.write(buffer, 0, read);
                }
            }
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(tempFile);
            throw exception;
        }
        return new TempUpload(tempFile, size, toHex(digest.digest()));
    }

    private MessageDigest newSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String toHex(byte[] digest) {
        StringBuilder stringBuilder = new StringBuilder(digest.length * 2);
        for (byte current : digest) {
            stringBuilder.append(Character.forDigit((current >> 4) & 0xF, 16));
            stringBuilder.append(Character.forDigit(current & 0xF, 16));
        }
        return stringBuilder.toString();
    }
}
