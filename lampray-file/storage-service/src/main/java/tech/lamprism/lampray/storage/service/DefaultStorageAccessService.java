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

package tech.lamprism.lampray.storage.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.StorageAccessRequest;
import tech.lamprism.lampray.storage.StorageDirectAccessMode;
import tech.lamprism.lampray.storage.StorageDownloadMode;
import tech.lamprism.lampray.storage.StorageDownloadResult;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageReference;
import tech.lamprism.lampray.storage.StorageReferenceMode;
import tech.lamprism.lampray.storage.StorageReferenceRequest;
import tech.lamprism.lampray.storage.StorageReferenceSource;
import tech.lamprism.lampray.storage.StorageVisibility;
import tech.lamprism.lampray.storage.builtin.BuiltinStorageRegistry;
import tech.lamprism.lampray.storage.builtin.BuiltinStorageResource;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.configuration.StorageTopology;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementRepository;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.routing.StorageGroupRouter;
import tech.lamprism.lampray.storage.store.BlobDownloadRequest;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.store.BlobStoreCapability;
import tech.lamprism.lampray.storage.store.BlobStoreRegistry;
import tech.lamprism.lampray.web.ExternalEndpointProvider;
import tech.rollw.common.web.AuthErrorCode;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.DataErrorCode;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author RollW
 */
@Service
@Transactional(readOnly = true)
public class DefaultStorageAccessService implements StorageAccessService {
    private final StorageTopology storageTopology;
    private final StorageRuntimeConfig runtimeSettings;
    private final BlobStoreRegistry blobStoreRegistry;
    private final StorageFileRepository storageFileRepository;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final ExternalEndpointProvider externalEndpointProvider;
    private final BuiltinStorageRegistry builtinStorageRegistry;
    private final StorageGroupRouter storageGroupRouter;
    private final StorageTransferPolicy storageTransferPolicy;
    private final StorageContentPolicy storageContentPolicy;

    public DefaultStorageAccessService(StorageTopology storageTopology,
                                       StorageRuntimeConfig runtimeSettings,
                                       BlobStoreRegistry blobStoreRegistry,
                                       StorageFileRepository storageFileRepository,
                                       StorageBlobPlacementRepository storageBlobPlacementRepository,
                                       ExternalEndpointProvider externalEndpointProvider,
                                       BuiltinStorageRegistry builtinStorageRegistry,
                                       StorageGroupRouter storageGroupRouter,
                                       StorageTransferPolicy storageTransferPolicy,
                                       StorageContentPolicy storageContentPolicy) {
        this.storageTopology = storageTopology;
        this.runtimeSettings = runtimeSettings;
        this.blobStoreRegistry = blobStoreRegistry;
        this.storageFileRepository = storageFileRepository;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.externalEndpointProvider = externalEndpointProvider;
        this.builtinStorageRegistry = builtinStorageRegistry;
        this.storageGroupRouter = storageGroupRouter;
        this.storageTransferPolicy = storageTransferPolicy;
        this.storageContentPolicy = storageContentPolicy;
    }

    @Override
    public StorageDownloadResult resolveDownload(String fileId,
                                                 Long userId) throws IOException {
        BuiltinStorageResource builtinResource = findBuiltinResource(fileId);
        if (builtinResource != null) {
            return new StorageDownloadResult(
                    builtinResource.fileStorage(),
                    StorageDownloadMode.PROXY,
                    null,
                    builtinResource.content()
            );
        }

        ResolvedDownloadTarget target = resolveStoredTarget(fileId, userId);
        String mimeType = storageContentPolicy.normalizeMimeType(target.fileStorage.getMimeType());
        StorageDownloadMode mode = storageTransferPolicy.resolveDownloadMode(
                target.fileStorage,
                target.groupConfig,
                target.blobStore
        );
        if (mode == StorageDownloadMode.DIRECT && !storageContentPolicy.isUnsafeDirectMimeType(mimeType)) {
            StorageAccessRequest directRequest = target.blobStore.createDirectDownload(
                    new BlobDownloadRequest(
                            target.placementEntity.getObjectKey(),
                            target.fileStorage.getFileName(),
                            mimeType
                    ),
                    Duration.ofSeconds(runtimeSettings.directAccessTtlSeconds())
            );
            if (isSimpleDirectDownload(directRequest)) {
                return new StorageDownloadResult(target.fileStorage, StorageDownloadMode.DIRECT, directRequest, null);
            }
        }

        return new StorageDownloadResult(
                target.fileStorage,
                StorageDownloadMode.PROXY,
                null,
                target.blobStore.openDownload(target.placementEntity.getObjectKey())
        );
    }

    @Override
    public StorageReference resolveStorageReference(String id,
                                                    StorageReferenceRequest request,
                                                    Long userId) throws IOException {
        StorageReferenceRequest normalizedRequest = request != null ? request : StorageReferenceRequest.proxy();
        if (normalizedRequest.getMode() == StorageReferenceMode.PROXY) {
            return proxyReference(id);
        }

        BuiltinStorageResource builtinResource = findBuiltinResource(id);
        if (builtinResource != null) {
            return resolveBuiltinReference(id, normalizedRequest, builtinResource);
        }

        ResolvedDownloadTarget target = resolveStoredTarget(id, userId);
        return resolveStoredReference(id, normalizedRequest, target);
    }

    private StorageReference resolveBuiltinReference(String fileId,
                                                     StorageReferenceRequest request,
                                                     BuiltinStorageResource builtinResource) {
        boolean canUseStaticDirect = request.getDirectAccessMode() != StorageDirectAccessMode.SIGNED;
        if (runtimeSettings.directAccessEnabled()
                && canUseStaticDirect
                && StringUtils.hasText(builtinResource.publicUrlPath())) {
            return new StorageReference(
                    buildExternalWebUrl(builtinResource.publicUrlPath()),
                    StorageDownloadMode.DIRECT,
                    StorageReferenceSource.CLASSPATH_STATIC,
                    Map.of(),
                    null
            );
        }
        if (request.getFallbackToProxy() || request.getMode() == StorageReferenceMode.AUTO) {
            return proxyReference(fileId);
        }
        throw new StorageException(
                CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                "Direct storage reference is not available: " + fileId
        );
    }

    private StorageReference resolveStoredReference(String fileId,
                                                    StorageReferenceRequest request,
                                                    ResolvedDownloadTarget target) throws IOException {
        if (!runtimeSettings.directAccessEnabled()) {
            if (request.getFallbackToProxy() || request.getMode() == StorageReferenceMode.AUTO) {
                return proxyReference(fileId);
            }
            throw new StorageException(
                    CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Direct storage reference is disabled: " + fileId
            );
        }

        if (request.getMode() == StorageReferenceMode.AUTO) {
            StorageDownloadMode autoMode = storageTransferPolicy.resolveDownloadMode(
                    target.fileStorage,
                    target.groupConfig,
                    target.blobStore
            );
            if (autoMode != StorageDownloadMode.DIRECT) {
                return proxyReference(fileId);
            }
        }

        StorageReference directReference;
        try {
            directReference = resolveDirectReference(target, request.getDirectAccessMode(), request.getTtlSeconds());
        } catch (IOException exception) {
            if (request.getFallbackToProxy() || request.getMode() == StorageReferenceMode.AUTO) {
                return proxyReference(fileId);
            }
            throw exception;
        }
        if (directReference != null) {
            return directReference;
        }
        if (request.getFallbackToProxy() || request.getMode() == StorageReferenceMode.AUTO) {
            return proxyReference(fileId);
        }
        throw new StorageException(
                CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                "Direct storage reference is not available: " + fileId
        );
    }

    private StorageReference resolveDirectReference(ResolvedDownloadTarget target,
                                                    StorageDirectAccessMode directAccessMode,
                                                    Long ttlSeconds) throws IOException {
        if (directAccessMode == StorageDirectAccessMode.PUBLIC) {
            return resolvePublicReference(target);
        }
        if (directAccessMode == StorageDirectAccessMode.SIGNED) {
            return resolveSignedReference(target, ttlSeconds);
        }

        StorageReference publicReference = resolvePublicReference(target);
        if (publicReference != null) {
            return publicReference;
        }
        return resolveSignedReference(target, ttlSeconds);
    }

    private StorageReference resolvePublicReference(ResolvedDownloadTarget target) throws IOException {
        String mimeType = storageContentPolicy.normalizeMimeType(target.fileStorage.getMimeType());
        if (target.visibility != StorageVisibility.PUBLIC) {
            return null;
        }
        if (!target.blobStore.supports(BlobStoreCapability.PUBLIC_DOWNLOAD_URL)) {
            return null;
        }
        if (storageContentPolicy.isUnsafeDirectMimeType(mimeType)) {
            return null;
        }
        String url = target.blobStore.createPublicDownloadUrl(
                new BlobDownloadRequest(target.placementEntity.getObjectKey(), target.fileStorage.getFileName(), mimeType)
        );
        return new StorageReference(
                url,
                StorageDownloadMode.DIRECT,
                StorageReferenceSource.BLOB_PUBLIC,
                Map.of(),
                null
        );
    }

    private StorageReference resolveSignedReference(ResolvedDownloadTarget target,
                                                    Long ttlSeconds) throws IOException {
        String mimeType = storageContentPolicy.normalizeMimeType(target.fileStorage.getMimeType());
        if (!target.blobStore.supports(BlobStoreCapability.DIRECT_DOWNLOAD)) {
            return null;
        }
        if (storageContentPolicy.isUnsafeDirectMimeType(mimeType)) {
            return null;
        }
        StorageAccessRequest accessRequest = target.blobStore.createDirectDownload(
                new BlobDownloadRequest(
                        target.placementEntity.getObjectKey(),
                        target.fileStorage.getFileName(),
                        mimeType
                ),
                Duration.ofSeconds(normalizeReferenceTtlSeconds(ttlSeconds))
        );
        return new StorageReference(
                accessRequest.getUrl(),
                StorageDownloadMode.DIRECT,
                StorageReferenceSource.BLOB_SIGNED,
                accessRequest.getHeaders(),
                accessRequest.getExpiresAt()
        );
    }

    private long normalizeReferenceTtlSeconds(Long ttlSeconds) {
        long resolved = ttlSeconds != null ? ttlSeconds : runtimeSettings.directAccessTtlSeconds();
        if (resolved <= 0) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "Storage reference ttl must be positive.");
        }
        return resolved;
    }

    private StorageReference proxyReference(String fileId) {
        return new StorageReference(
                buildApiUrl(fileId),
                StorageDownloadMode.PROXY,
                StorageReferenceSource.API,
                Map.of(),
                null
        );
    }

    private BuiltinStorageResource findBuiltinResource(String fileId) {
        if (!builtinStorageRegistry.contains(fileId)) {
            return null;
        }
        return builtinStorageRegistry.get(fileId);
    }

    private ResolvedDownloadTarget resolveStoredTarget(String fileId,
                                                       Long userId) {
        StorageFileEntity fileEntity = requireFileEntity(fileId);
        ensureDownloadAuthorized(fileEntity, userId);
        StorageGroupConfig groupConfig = storageTopology.getGroup(fileEntity.getGroupName());
        StorageBlobPlacementEntity placementEntity = resolvePlacement(fileEntity.getBlobId(), groupConfig);
        BlobStore blobStore = requireBlobStore(placementEntity.getBackendName());
        return new ResolvedDownloadTarget(fileEntity.lock(), fileEntity.getVisibility(), groupConfig, placementEntity, blobStore);
    }

    private StorageFileEntity requireFileEntity(String fileId) {
        return storageFileRepository.findById(fileId)
                .orElseThrow(() -> new StorageException(
                        DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "File not found: " + fileId
                ));
    }

    private BlobStore requireBlobStore(String backendName) {
        return blobStoreRegistry.find(backendName)
                .orElseThrow(() -> new StorageException(
                        DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "Storage backend is not available: " + backendName
                ));
    }

    private StorageBlobPlacementEntity resolvePlacement(String blobId,
                                                        StorageGroupConfig groupConfig) {
        List<StorageBlobPlacementEntity> placements = storageBlobPlacementRepository.findAllByBlobId(blobId);
        if (placements.isEmpty()) {
            throw new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST, "No blob placement found for blob: " + blobId);
        }
        Map<String, StorageBlobPlacementEntity> placementsByBackend = placements.stream()
                .collect(java.util.stream.Collectors.toMap(
                        StorageBlobPlacementEntity::getBackendName,
                        placement -> placement,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        String selectedBackend;
        try {
            selectedBackend = storageGroupRouter.selectReadBackend(groupConfig.getName(), placementsByBackend.keySet());
        } catch (IllegalStateException exception) {
            throw new StorageException(
                    DataErrorCode.ERROR_DATA_NOT_EXIST,
                    "No active blob store backend available for blob: " + blobId
            );
        }
        return placementsByBackend.getOrDefault(selectedBackend, placements.get(0));
    }

    private void ensureDownloadAuthorized(StorageFileEntity fileEntity,
                                          Long userId) {
        if (fileEntity.getVisibility() == StorageVisibility.PUBLIC) {
            return;
        }
        if (fileEntity.getVisibility() == StorageVisibility.INTERNAL && userId != null) {
            return;
        }
        if (fileEntity.getVisibility() == StorageVisibility.PRIVATE
                && userId != null
                && userId.equals(fileEntity.getOwnerUserId())) {
            return;
        }
        throw new StorageException(AuthErrorCode.ERROR_UNAUTHORIZED_USE, "You are not allowed to access this file.");
    }

    private String buildApiUrl(String fileId) {
        return joinUrl(externalEndpointProvider.getExternalApiEndpoint(), "/api/v1/files/" + fileId);
    }

    private String buildExternalWebUrl(String path) {
        return joinUrl(externalEndpointProvider.getExternalWebEndpoint(), path);
    }

    private String joinUrl(String endpoint,
                           String path) {
        String normalizedEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedEndpoint + normalizedPath;
    }

    private boolean isSimpleDirectDownload(StorageAccessRequest request) {
        return request != null
                && "GET".equalsIgnoreCase(request.getMethod())
                && request.getHeaders().isEmpty();
    }

    private static final class ResolvedDownloadTarget {
        private final FileStorage fileStorage;
        private final StorageVisibility visibility;
        private final StorageGroupConfig groupConfig;
        private final StorageBlobPlacementEntity placementEntity;
        private final BlobStore blobStore;

        private ResolvedDownloadTarget(FileStorage fileStorage,
                                       StorageVisibility visibility,
                                       StorageGroupConfig groupConfig,
                                       StorageBlobPlacementEntity placementEntity,
                                       BlobStore blobStore) {
            this.fileStorage = fileStorage;
            this.visibility = visibility;
            this.groupConfig = groupConfig;
            this.placementEntity = placementEntity;
            this.blobStore = blobStore;
        }
    }
}
