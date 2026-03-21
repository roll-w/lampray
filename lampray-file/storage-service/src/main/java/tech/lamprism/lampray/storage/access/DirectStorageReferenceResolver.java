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
import tech.lamprism.lampray.storage.StorageDirectAccessMode;
import tech.lamprism.lampray.storage.StorageDownloadMode;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageReference;
import tech.lamprism.lampray.storage.StorageReferenceRequest;
import tech.lamprism.lampray.storage.StorageReferenceSource;
import tech.lamprism.lampray.storage.StorageVisibility;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.monitoring.StorageTrafficRecorder;
import tech.lamprism.lampray.storage.policy.StorageContentPolicy;
import tech.lamprism.lampray.storage.policy.StorageTransferPolicy;
import tech.lamprism.lampray.storage.store.BlobDownloadRequest;
import tech.lamprism.lampray.storage.store.BlobStoreCapability;
import tech.rollw.common.web.CommonErrorCode;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Component
class DirectStorageReferenceResolver {
    private final StorageRuntimeConfig runtimeSettings;
    private final StorageTransferPolicy storageTransferPolicy;
    private final StorageContentPolicy storageContentPolicy;
    private final StorageTrafficRecorder storageTrafficRecorder;

    DirectStorageReferenceResolver(StorageRuntimeConfig runtimeSettings,
                                   StorageTransferPolicy storageTransferPolicy,
                                   StorageContentPolicy storageContentPolicy,
                                   StorageTrafficRecorder storageTrafficRecorder) {
        this.runtimeSettings = runtimeSettings;
        this.storageTransferPolicy = storageTransferPolicy;
        this.storageContentPolicy = storageContentPolicy;
        this.storageTrafficRecorder = storageTrafficRecorder;
    }

    StorageReference resolve(StoredDownloadTarget target,
                             StorageReferenceRequest request) throws IOException {
        if (!runtimeSettings.getDirectAccessEnabled()) {
            return null;
        }

        if (request.getMode() == tech.lamprism.lampray.storage.StorageReferenceMode.AUTO) {
            StorageDownloadMode autoMode = storageTransferPolicy.resolveDownloadMode(
                    target.fileStorage(),
                    target.groupConfig(),
                    target.blobStore()
            );
            if (autoMode != StorageDownloadMode.DIRECT) {
                return null;
            }
        }

        if (request.getDirectAccessMode() == StorageDirectAccessMode.PUBLIC) {
            StorageReference reference = resolvePublicReference(target);
            if (reference != null) {
                storageTrafficRecorder.recordDirectDownloadRequest(target.groupConfig().getName(), target.placementEntity().getBackendName());
            }
            return reference;
        }
        if (request.getDirectAccessMode() == StorageDirectAccessMode.SIGNED) {
            StorageReference reference = resolveSignedReference(target, request.getTtlSeconds());
            if (reference != null) {
                storageTrafficRecorder.recordDirectDownloadRequest(target.groupConfig().getName(), target.placementEntity().getBackendName());
            }
            return reference;
        }

        StorageReference publicReference = resolvePublicReference(target);
        if (publicReference != null) {
            storageTrafficRecorder.recordDirectDownloadRequest(target.groupConfig().getName(), target.placementEntity().getBackendName());
            return publicReference;
        }
        StorageReference signedReference = resolveSignedReference(target, request.getTtlSeconds());
        if (signedReference != null) {
            storageTrafficRecorder.recordDirectDownloadRequest(target.groupConfig().getName(), target.placementEntity().getBackendName());
        }
        return signedReference;
    }

    private StorageReference resolvePublicReference(StoredDownloadTarget target) throws IOException {
        String mimeType = storageContentPolicy.normalizeMimeType(target.fileStorage().getMimeType());
        if (target.visibility() != StorageVisibility.PUBLIC) {
            return null;
        }
        if (!target.blobStore().supports(BlobStoreCapability.PUBLIC_DOWNLOAD_URL)) {
            return null;
        }
        if (storageContentPolicy.isUnsafeDirectMimeType(mimeType)) {
            return null;
        }
        String url = target.blobStore().createPublicDownloadUrl(
                new BlobDownloadRequest(target.placementEntity().getObjectKey(), target.fileStorage().getFileName(), mimeType)
        );
        return new StorageReference(
                url,
                StorageDownloadMode.DIRECT,
                StorageReferenceSource.BLOB_PUBLIC,
                Map.of(),
                null
        );
    }

    private StorageReference resolveSignedReference(StoredDownloadTarget target,
                                                    Long ttlSeconds) throws IOException {
        String mimeType = storageContentPolicy.normalizeMimeType(target.fileStorage().getMimeType());
        if (!target.blobStore().supports(BlobStoreCapability.DIRECT_DOWNLOAD)) {
            return null;
        }
        if (storageContentPolicy.isUnsafeDirectMimeType(mimeType)) {
            return null;
        }
        StorageAccessRequest accessRequest = target.blobStore().createDirectDownload(
                new BlobDownloadRequest(
                        target.placementEntity().getObjectKey(),
                        target.fileStorage().getFileName(),
                        mimeType
                ),
                Duration.ofSeconds(normalizeReferenceTtlSeconds(ttlSeconds))
        );
        if (!isSimpleDirectDownload(accessRequest)) {
            return new StorageReference(
                    accessRequest.getUrl(),
                    StorageDownloadMode.DIRECT,
                    StorageReferenceSource.BLOB_SIGNED,
                    accessRequest.getHeaders(),
                    accessRequest.getExpiresAt()
            );
        }
        return new StorageReference(
                accessRequest.getUrl(),
                StorageDownloadMode.DIRECT,
                StorageReferenceSource.BLOB_SIGNED,
                accessRequest.getHeaders(),
                accessRequest.getExpiresAt()
        );
    }

    private long normalizeReferenceTtlSeconds(Long ttlSeconds) {
        long resolved = ttlSeconds != null ? ttlSeconds : runtimeSettings.getDirectAccessTtlSeconds();
        if (resolved <= 0) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "Storage reference ttl must be positive.");
        }
        return resolved;
    }

    private boolean isSimpleDirectDownload(StorageAccessRequest request) {
        return request != null
                && "GET".equalsIgnoreCase(request.getMethod())
                && request.getHeaders().isEmpty();
    }
}
