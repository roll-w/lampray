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

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.support.PathCleanupSupport;
import tech.rollw.common.web.CommonErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class TempUploads {
    private static final int BUFFER_SIZE = 8192;
    private static final String TEMP_FILE_PREFIX = "lampray-upload-";
    private static final String TEMP_FILE_SUFFIX = ".tmp";

    private TempUploads() {
    }

    public static TempUpload write(InputStream inputStream,
                                   Long maxSizeBytes) throws IOException {
        Path tempFile = createTempFilePath();
        Hasher hasher = Hashing.sha256().newHasher();
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
                    hasher.putBytes(buffer, 0, read);
                    outputStream.write(buffer, 0, read);
                }
            }
        } catch (IOException | RuntimeException exception) {
            PathCleanupSupport.deleteIfExistsQuietly(tempFile);
            throw exception;
        }
        return new TempUpload(tempFile, size, hasher.hash().toString());
    }

    private static Path createTempFilePath() throws IOException {
        return Files.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
    }
}
