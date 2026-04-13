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

package tech.lamprism.lampray.storage.access.resolver;

import com.google.common.collect.Maps;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.StorageDownloadMode;
import tech.lamprism.lampray.storage.StorageDownloadResult;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageReference;
import tech.lamprism.lampray.storage.StorageReferenceMode;
import tech.lamprism.lampray.storage.StorageReferenceRequest;
import tech.lamprism.lampray.storage.StorageVisibility;
import tech.lamprism.lampray.storage.access.StorageAccessResolver;
import tech.lamprism.lampray.storage.access.model.StoredBlobPlacement;
import tech.lamprism.lampray.storage.access.model.StoredDownloadTarget;
import tech.lamprism.lampray.storage.access.strategy.StoredAccessStrategy;
import tech.lamprism.lampray.storage.backend.BlobStoreLocator;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.configuration.StorageTopology;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementRepository;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.policy.StorageTransferModeResolver;
import tech.lamprism.lampray.storage.routing.StorageGroupRouter;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.rollw.common.web.AuthErrorCode;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.DataErrorCode;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolves access for persisted storage files.
 *
 * @author RollW
 */
@Component
@Order(1)
public class StoredStorageAccessResolver implements StorageAccessResolver {
    private final StorageRuntimeConfig runtimeSettings;
    private final StorageTransferModeResolver transferModeResolver;
    private final StorageTopology storageTopology;
    private final BlobStoreLocator blobStoreLocator;
    private final StorageFileRepository storageFileRepository;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final StorageGroupRouter storageGroupRouter;
    private final Map<StorageDownloadMode, StoredAccessStrategy> accessStrategies;

    public StoredStorageAccessResolver(StorageRuntimeConfig runtimeSettings,
                                       StorageTopology storageTopology,
                                       BlobStoreLocator blobStoreLocator,
                                       StorageFileRepository storageFileRepository,
                                       StorageBlobPlacementRepository storageBlobPlacementRepository,
                                       StorageGroupRouter storageGroupRouter,
                                       List<StoredAccessStrategy> accessStrategies) {
        this.runtimeSettings = runtimeSettings;
        this.transferModeResolver = new StorageTransferModeResolver(runtimeSettings);
        this.storageTopology = storageTopology;
        this.blobStoreLocator = blobStoreLocator;
        this.storageFileRepository = storageFileRepository;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.storageGroupRouter = storageGroupRouter;
        this.accessStrategies = Maps.uniqueIndex(accessStrategies, StoredAccessStrategy::mode);
    }

    @Override
    public StorageDownloadResult resolveDownload(String fileId,
                                                 Long userId) throws IOException {
        StoredDownloadTarget target = resolveTarget(fileId, userId);
        StorageDownloadMode mode = transferModeResolver.resolveDownloadMode(
                target.getFileStorage(),
                target.getGroupConfig(),
                target.getBlobStore()
        );
        StoredAccessStrategy strategy = accessStrategy(mode);
        StorageDownloadResult directOrProxy = strategy.resolveDownload(target);
        if (directOrProxy != null) {
            return directOrProxy;
        }
        return accessStrategy(StorageDownloadMode.PROXY).resolveDownload(target);
    }

    @Override
    public StorageReference resolveReference(String fileId,
                                             StorageReferenceRequest request,
                                             Long userId) throws IOException {
        StoredDownloadTarget target = resolveTarget(fileId, userId);
        StorageDownloadMode resolvedMode = transferModeResolver.resolveDownloadMode(
                target.getFileStorage(),
                target.getGroupConfig(),
                target.getBlobStore()
        );
        if (!runtimeSettings.getDirectAccessEnabled()) {
            if (request.getFallbackToProxy() || request.getMode() == StorageReferenceMode.AUTO) {
                return accessStrategy(StorageDownloadMode.PROXY).resolveReference(fileId, target, request);
            }
            throw new StorageException(
                    CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Direct storage reference is disabled: " + fileId
            );
        }

        if (resolvedMode != StorageDownloadMode.DIRECT) {
            if (request.getFallbackToProxy() || request.getMode() == StorageReferenceMode.AUTO) {
                return accessStrategy(StorageDownloadMode.PROXY).resolveReference(fileId, target, request);
            }
            throw new StorageException(
                    CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Direct storage reference is not available: " + fileId
            );
        }

        StorageReference directReference;
        try {
            directReference = accessStrategy(StorageDownloadMode.DIRECT).resolveReference(fileId, target, request);
        } catch (IOException exception) {
            if (request.getFallbackToProxy() || request.getMode() == StorageReferenceMode.AUTO) {
                return accessStrategy(StorageDownloadMode.PROXY).resolveReference(fileId, target, request);
            }
            throw exception;
        }
        if (directReference != null) {
            return directReference;
        }
        if (request.getFallbackToProxy() || request.getMode() == StorageReferenceMode.AUTO) {
            return accessStrategy(StorageDownloadMode.PROXY).resolveReference(fileId, target, request);
        }
        throw new StorageException(
                CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                "Direct storage reference is not available: " + fileId
        );
    }

    public StorageReference proxyReference(String fileId) throws IOException {
        return accessStrategy(StorageDownloadMode.PROXY).resolveReference(fileId, null, new StorageReferenceRequest());
    }

    private StoredAccessStrategy accessStrategy(StorageDownloadMode mode) {
        StoredAccessStrategy strategy = accessStrategies.get(mode);
        if (strategy == null) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Unsupported storage access mode: " + mode);
        }
        return strategy;
    }

    private StoredDownloadTarget resolveTarget(String fileId,
                                               Long userId) {
        StorageFileEntity fileEntity = requireFileEntity(fileId);
        ensureDownloadAuthorized(fileEntity, userId);
        StorageGroupConfig groupConfig = storageTopology.getGroup(fileEntity.getGroupName());
        StorageBlobPlacementEntity placementEntity = resolvePlacement(fileEntity.getBlobId(), groupConfig);
        StoredBlobPlacement placement = new StoredBlobPlacement(placementEntity.getBackendName(), placementEntity.getObjectKey());
        BlobStore blobStore = blobStoreLocator.require(placement.getBackendName());
        return new StoredDownloadTarget(toFileStorage(fileEntity), fileEntity.getVisibility(), groupConfig, placement, blobStore);
    }

    private StorageFileEntity requireFileEntity(String fileId) {
        return storageFileRepository.findActiveById(fileId)
                .orElseThrow(() -> new StorageException(
                        DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "File not found: " + fileId
                ));
    }

    private StorageBlobPlacementEntity resolvePlacement(String blobId,
                                                        StorageGroupConfig groupConfig) {
        List<StorageBlobPlacementEntity> placements = storageBlobPlacementRepository.findAllByBlobId(blobId);
        if (placements.isEmpty()) {
            throw new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST, "No blob placement found for blob: " + blobId);
        }
        Map<String, StorageBlobPlacementEntity> placementsByBackend = placements.stream()
                .collect(Collectors.toMap(
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

    private FileStorage toFileStorage(StorageFileEntity fileEntity) {
        return new FileStorage(
                fileEntity.getFileId(),
                fileEntity.getFileName(),
                fileEntity.getFileSize(),
                fileEntity.getMimeType(),
                fileEntity.getFileType(),
                fileEntity.getCreateTime()
        );
    }

}
