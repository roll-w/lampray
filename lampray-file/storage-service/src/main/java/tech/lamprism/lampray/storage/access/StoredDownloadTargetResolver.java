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
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageVisibility;
import tech.lamprism.lampray.storage.backend.BlobStoreLocator;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageTopology;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementRepository;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.routing.StorageGroupRouter;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.rollw.common.web.AuthErrorCode;
import tech.rollw.common.web.DataErrorCode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the stored blob target selected for download access.
 *
 * @author RollW
 */
@Component
public class StoredDownloadTargetResolver {
    private final StorageTopology storageTopology;
    private final BlobStoreLocator blobStoreLocator;
    private final StorageFileRepository storageFileRepository;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final StorageGroupRouter storageGroupRouter;

    public StoredDownloadTargetResolver(StorageTopology storageTopology,
                                        BlobStoreLocator blobStoreLocator,
                                        StorageFileRepository storageFileRepository,
                                        StorageBlobPlacementRepository storageBlobPlacementRepository,
                                        StorageGroupRouter storageGroupRouter) {
        this.storageTopology = storageTopology;
        this.blobStoreLocator = blobStoreLocator;
        this.storageFileRepository = storageFileRepository;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.storageGroupRouter = storageGroupRouter;
    }

    public StoredDownloadTarget resolve(String fileId,
                                        Long userId) {
        StorageFileEntity fileEntity = requireFileEntity(fileId);
        ensureDownloadAuthorized(fileEntity, userId);
        StorageGroupConfig groupConfig = storageTopology.getGroup(fileEntity.getGroupName());
        StorageBlobPlacementEntity placementEntity = resolvePlacement(fileEntity.getBlobId(), groupConfig);
        BlobStore blobStore = blobStoreLocator.require(placementEntity.getBackendName());
        return new StoredDownloadTarget(fileEntity.lock(), fileEntity.getVisibility(), groupConfig, placementEntity, blobStore);
    }

    private StorageFileEntity requireFileEntity(String fileId) {
        return storageFileRepository.findById(fileId)
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
}
