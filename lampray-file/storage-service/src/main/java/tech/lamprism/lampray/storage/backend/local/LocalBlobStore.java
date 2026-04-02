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

package tech.lamprism.lampray.storage.backend.local;

import tech.lamprism.lampray.storage.StorageDownloadSource;
import tech.lamprism.lampray.storage.source.InputStreamDownloadSource;
import tech.lamprism.lampray.storage.store.BlobObject;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.store.BlobWriteRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * @author RollW
 */
public class LocalBlobStore implements BlobStore {
    private final String backendName;
    private final Path rootPath;
    private final String rootPrefix;

    public LocalBlobStore(String backendName,
                          Path rootPath,
                          String rootPrefix) throws IOException {
        this.backendName = backendName;
        this.rootPath = rootPath.toAbsolutePath().normalize();
        this.rootPrefix = normalizeRootPrefix(rootPrefix);
        Files.createDirectories(this.rootPath);
    }

    @Override
    public String getBackendName() {
        return backendName;
    }

    @Override
    public BlobObject store(BlobWriteRequest request,
                            InputStream inputStream) throws IOException {
        Path resolved = resolve(request.getKey());
        if (resolved.getParent() != null) {
            Files.createDirectories(resolved.getParent());
        }
        Files.copy(inputStream, resolved, StandardCopyOption.REPLACE_EXISTING);
        return describeInternal(
                resolved,
                request.getKey(),
                request.getContentType(),
                request.getMetadata(),
                request.getContentChecksum()
        );
    }

    @Override
    public StorageDownloadSource openDownload(String key) throws IOException {
        Path resolved = resolve(key);
        return InputStreamDownloadSource.fromPath(resolved);
    }

    @Override
    public BlobObject describe(String key) throws IOException {
        Path resolved = resolve(key);
        return describeInternal(resolved, key, Files.probeContentType(resolved), Map.of(), null);
    }

    @Override
    public boolean exists(String key) throws IOException {
        return Files.exists(resolve(key));
    }

    @Override
    public boolean delete(String key) throws IOException {
        return Files.deleteIfExists(resolve(key));
    }

    private Path resolve(String key) throws IOException {
        Path resolved = rootPath.resolve(toStorageKey(key)).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new IOException("Invalid local blob key: " + key);
        }
        return resolved;
    }

    private String toStorageKey(String key) {
        if (rootPrefix.isEmpty()) {
            return key;
        }
        return rootPrefix + "/" + key;
    }

    private String normalizeRootPrefix(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private BlobObject describeInternal(Path path,
                                        String key,
                                        String contentType,
                                        Map<String, String> metadata,
                                        String contentChecksum) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Blob does not exist: " + key);
        }
        FileTime fileTime = Files.getLastModifiedTime(path);
        return new BlobObject(
                backendName,
                key,
                Files.size(path),
                contentType != null ? contentType : "application/octet-stream",
                null,
                contentChecksum,
                OffsetDateTime.ofInstant(fileTime.toInstant(), ZoneOffset.UTC),
                metadata
        );
    }
}
