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

package tech.lamprism.lampray.storage.access;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.StorageAccessRequest;
import tech.lamprism.lampray.storage.StorageDownloadMode;
import tech.lamprism.lampray.storage.StorageDownloadResult;
import tech.lamprism.lampray.storage.StorageReference;
import tech.lamprism.lampray.storage.StorageReferenceRequest;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.monitoring.StorageTrafficPublisher;
import tech.lamprism.lampray.storage.policy.StorageContentRules;
import tech.lamprism.lampray.storage.store.BlobDownloadRequest;

import java.io.IOException;
import java.time.Duration;

/**
 * Implements direct access for stored files.
 *
 * @author RollW
 */
@Component
public class DirectStoredAccessStrategy implements StoredAccessStrategy {

    private final StorageRuntimeConfig runtimeSettings;
    private final DirectStorageReferenceResolver directStorageReferenceResolver;
    private final StorageTrafficPublisher storageTrafficPublisher;

    public DirectStoredAccessStrategy(StorageRuntimeConfig runtimeSettings,
                                      DirectStorageReferenceResolver directStorageReferenceResolver,
                                      StorageTrafficPublisher storageTrafficPublisher) {
        this.runtimeSettings = runtimeSettings;
        this.directStorageReferenceResolver = directStorageReferenceResolver;
        this.storageTrafficPublisher = storageTrafficPublisher;
    }

    @Override
    public StorageDownloadMode mode() {
        return StorageDownloadMode.DIRECT;
    }

    @Override
    public StorageDownloadResult resolveDownload(StoredDownloadTarget target) throws IOException {
        String mimeType = StorageContentRules.normalizeMimeType(target.getFileStorage().getMimeType());
        if (StorageContentRules.isUnsafeDirectMimeType(mimeType)) {
            return null;
        }
        StorageAccessRequest directRequest = target.getBlobStore().createDirectDownload(
                new BlobDownloadRequest(
                        target.getPlacement().getObjectKey(),
                        target.getFileStorage().getFileName(),
                        mimeType
                ),
                Duration.ofSeconds(runtimeSettings.getDirectAccessTtlSeconds())
        );
        if (!isSimpleDirectDownload(directRequest)) {
            return null;
        }
        storageTrafficPublisher.publishDirectDownloadRequest(target.getGroupConfig().getName(), target.getPlacement().getBackendName());
        return new StorageDownloadResult(
                target.getFileStorage(),
                StorageDownloadMode.DIRECT,
                directRequest,
                null
        );
    }

    @Override
    public StorageReference resolveReference(String fileId,
                                             StoredDownloadTarget target,
                                             StorageReferenceRequest request) throws IOException {
        return directStorageReferenceResolver.resolve(target, request);
    }

    private boolean isSimpleDirectDownload(StorageAccessRequest request) {
        return request != null
                && "GET".equalsIgnoreCase(request.getMethod())
                && request.getHeaders().isEmpty();
    }
}
