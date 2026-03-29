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

package tech.lamprism.lampray.storage.materialization.placement;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.backend.BlobStoreLocator;
import tech.lamprism.lampray.storage.store.BlobWriteRequest;
import tech.lamprism.lampray.storage.support.BlobMetadataSupport;
import tech.lamprism.lampray.storage.support.PathCleanupSupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * @author RollW
 */
@Service
public class BlobStorePlacementWriter implements BlobPlacementWriter {
    private final BlobStoreLocator blobStoreLocator;

    public BlobStorePlacementWriter(BlobStoreLocator blobStoreLocator) {
        this.blobStoreLocator = blobStoreLocator;
    }

    @Override
    public void putTempToBackend(String backendName,
                                 String objectKey,
                                 Path tempPath,
                                 long size,
                                 String mimeType,
                                 String checksumSha256) throws IOException {
        try (InputStream inputStream = Files.newInputStream(Objects.requireNonNull(tempPath))) {
            blobStoreLocator.require(backendName).store(
                    new BlobWriteRequest(
                            objectKey,
                            size,
                            mimeType,
                            BlobMetadataSupport.checksumMetadata(checksumSha256),
                            checksumSha256
                    ),
                    inputStream
            );
        }
    }

    @Override
    public void replicateBetweenBackends(String sourceBackend,
                                         String sourceObjectKey,
                                         String targetBackend,
                                         String targetObjectKey,
                                         long size,
                                         String mimeType,
                                         String checksum) throws IOException {
        Path tempFile = Files.createTempFile("lampray-replica-", ".bin");
        try {
            try (OutputStream outputStream = Files.newOutputStream(tempFile, StandardOpenOption.TRUNCATE_EXISTING)) {
                blobStoreLocator.require(sourceBackend).openDownload(sourceObjectKey)
                        .transferTo(outputStream);
            }
            putTempToBackend(targetBackend, targetObjectKey, tempFile, size, mimeType, checksum);
        } finally {
            PathCleanupSupport.deleteIfExistsQuietly(tempFile);
        }
    }
}
