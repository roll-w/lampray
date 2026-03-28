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
import tech.lamprism.lampray.storage.monitoring.StorageTrafficPublisher;
import tech.lamprism.lampray.storage.policy.StorageContentRules;
import tech.lamprism.lampray.storage.policy.StorageTransferModeResolver;
import tech.lamprism.lampray.storage.store.BlobDownloadRequest;
import tech.lamprism.lampray.storage.store.BlobStoreCapability;
import tech.rollw.common.web.CommonErrorCode;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Resolves direct storage references for stored files.
 *
 * @author RollW
 */
@Component
public class DirectStorageReferenceResolver {
    private static final StorageContentRules contentRules = StorageContentRules.INSTANCE;

    private final StorageRuntimeConfig runtimeSettings;
    private final StorageTransferModeResolver transferModeResolver;
    private final StorageTrafficPublisher storageTrafficPublisher;

    public DirectStorageReferenceResolver(StorageRuntimeConfig runtimeSettings,
                                          StorageTrafficPublisher storageTrafficPublisher) {
        this.runtimeSettings = runtimeSettings;
        this.transferModeResolver = new StorageTransferModeResolver(runtimeSettings);
        this.storageTrafficPublisher = storageTrafficPublisher;
    }

    public StorageReference resolve(StoredDownloadTarget target,
                                    StorageReferenceRequest request) throws IOException {
        if (!runtimeSettings.getDirectAccessEnabled()) {
            return null;
        }

        if (request.getMode() == tech.lamprism.lampray.storage.StorageReferenceMode.AUTO) {
            StorageDownloadMode autoMode = transferModeResolver.resolveDownloadMode(
                    target.getFileStorage(),
                    target.getGroupConfig(),
                    target.getBlobStore()
            );
            if (autoMode != StorageDownloadMode.DIRECT) {
                return null;
            }
        }

        if (request.getDirectAccessMode() == StorageDirectAccessMode.PUBLIC) {
            return publishResolvedReference(target, resolvePublicReference(target));
        }
        if (request.getDirectAccessMode() == StorageDirectAccessMode.SIGNED) {
            return publishResolvedReference(target, resolveSignedReference(target, request.getTtlSeconds()));
        }

        StorageReference reference = publishResolvedReference(target, resolvePublicReference(target));
        if (reference != null) {
            return reference;
        }
        return publishResolvedReference(target, resolveSignedReference(target, request.getTtlSeconds()));
    }

    private StorageReference resolvePublicReference(StoredDownloadTarget target) throws IOException {
        String mimeType = contentRules.normalizeMimeType(target.getFileStorage().getMimeType());
        if (target.getVisibility() != StorageVisibility.PUBLIC) {
            return null;
        }
        if (!target.getBlobStore().supports(BlobStoreCapability.PUBLIC_DOWNLOAD_URL)) {
            return null;
        }
        if (contentRules.isUnsafeDirectMimeType(mimeType)) {
            return null;
        }
        String url = target.getBlobStore().createPublicDownloadUrl(
                new BlobDownloadRequest(target.getPlacementEntity().getObjectKey(), target.getFileStorage().getFileName(), mimeType)
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
        String mimeType = contentRules.normalizeMimeType(target.getFileStorage().getMimeType());
        if (!target.getBlobStore().supports(BlobStoreCapability.DIRECT_DOWNLOAD)) {
            return null;
        }
        if (contentRules.isUnsafeDirectMimeType(mimeType)) {
            return null;
        }
        StorageAccessRequest accessRequest = target.getBlobStore().createDirectDownload(
                new BlobDownloadRequest(
                        target.getPlacementEntity().getObjectKey(),
                        target.getFileStorage().getFileName(),
                        mimeType
                ),
                Duration.ofSeconds(normalizeReferenceTtlSeconds(ttlSeconds))
        );
        if (accessRequest == null) {
            return null;
        }
        return new StorageReference(
                accessRequest.getUrl(),
                StorageDownloadMode.DIRECT,
                StorageReferenceSource.BLOB_SIGNED,
                accessRequest.getHeaders(),
                accessRequest.getExpiresAt()
        );
    }

    private StorageReference publishResolvedReference(StoredDownloadTarget target,
                                                     StorageReference reference) {
        if (reference == null) {
            return null;
        }
        storageTrafficPublisher.publishDirectDownloadRequest(
                target.getGroupConfig().getName(),
                target.getPlacementEntity().getBackendName()
        );
        return reference;
    }

    private long normalizeReferenceTtlSeconds(Long ttlSeconds) {
        long resolved = ttlSeconds != null ? ttlSeconds : runtimeSettings.getDirectAccessTtlSeconds();
        if (resolved <= 0) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "Storage reference ttl must be positive.");
        }
        return resolved;
    }
}
