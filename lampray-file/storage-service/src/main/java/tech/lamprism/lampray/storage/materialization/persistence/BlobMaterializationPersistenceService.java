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

package tech.lamprism.lampray.storage.materialization.persistence;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tech.lamprism.lampray.common.data.ResourceIdGenerator;
import tech.lamprism.lampray.storage.StorageResourceKind;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementRepository;
import tech.lamprism.lampray.storage.persistence.StorageBlobRepository;
import tech.lamprism.lampray.storage.support.StorageBlobLifecycleLockManager;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * @author RollW
 */
@Service
public class BlobMaterializationPersistenceService {
    private final StorageBlobRepository storageBlobRepository;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final ResourceIdGenerator resourceIdGenerator;
    private final StorageBlobLifecycleLockManager storageBlobLifecycleLockManager;

    public BlobMaterializationPersistenceService(StorageBlobRepository storageBlobRepository,
                                                 StorageBlobPlacementRepository storageBlobPlacementRepository,
                                                 ResourceIdGenerator resourceIdGenerator,
                                                 StorageBlobLifecycleLockManager storageBlobLifecycleLockManager) {
        this.storageBlobRepository = storageBlobRepository;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.resourceIdGenerator = resourceIdGenerator;
        this.storageBlobLifecycleLockManager = storageBlobLifecycleLockManager;
    }

    public StorageBlobEntity persist(PreparedBlobMaterialization preparedBlob) {
        try (StorageBlobLifecycleLockManager.LockedKey ignored = storageBlobLifecycleLockManager.acquire(preparedBlob.getChecksum())) {
            StorageBlobEntity blobEntity = resolveExistingBlob(preparedBlob);
            OffsetDateTime now = OffsetDateTime.now();
            if (blobEntity == null) {
                StorageBlobEntity candidate = StorageBlobEntity.builder()
                        .setBlobId(newBlobId())
                        .setContentChecksum(preparedBlob.getChecksum())
                        .setFileSize(preparedBlob.getSize())
                        .setMimeType(preparedBlob.getMimeType())
                        .setFileType(preparedBlob.getFileType())
                        .setPrimaryBackend(preparedBlob.getPrimaryBackend())
                        .setPrimaryObjectKey(preparedBlob.getPrimaryObjectKey())
                        .setCreateTime(now)
                        .setUpdateTime(now)
                        .build();
                try {
                    blobEntity = storageBlobRepository.save(candidate);
                } catch (DataIntegrityViolationException exception) {
                    blobEntity = storageBlobRepository.findByContentChecksum(preparedBlob.getChecksum())
                            .orElseThrow(() -> exception);
                }
            } else if (blobEntity.getOrphanedAt() != null) {
                StorageBlobEntity revivedBlob = blobEntity.toBuilder()
                        .setOrphanedAt(null)
                        .setUpdateTime(now)
                        .build();
                blobEntity = storageBlobRepository.save(revivedBlob);
            }

            for (Map.Entry<String, String> entry : preparedBlob.getPlacementsToPersist().entrySet()) {
                persistPlacementIfAbsent(blobEntity.getBlobId(), entry.getKey(), entry.getValue(), now);
            }
            return blobEntity;
        }
    }

    private StorageBlobEntity resolveExistingBlob(PreparedBlobMaterialization preparedBlob) {
        StorageBlobEntity existingBlob = preparedBlob.getExistingBlob();
        if (existingBlob == null) {
            return null;
        }
        return storageBlobRepository.findById(existingBlob.getBlobId())
                .or(() -> storageBlobRepository.findByContentChecksum(preparedBlob.getChecksum()))
                .orElse(null);
    }

    private void persistPlacementIfAbsent(String blobId,
                                          String backendName,
                                          String objectKey,
                                          OffsetDateTime now) {
        if (storageBlobPlacementRepository.findByBlobIdAndBackendName(blobId, backendName).isPresent()) {
            return;
        }
        StorageBlobPlacementEntity existingPlacement = storageBlobPlacementRepository
                .findAnyByBlobIdAndBackendName(blobId, backendName)
                .orElse(null);
        if (existingPlacement != null) {
            StorageBlobPlacementEntity revivedPlacement = existingPlacement.toBuilder()
                    .setObjectKey(objectKey)
                    .setDeleted(false)
                    .setUpdateTime(now)
                    .build();
            storageBlobPlacementRepository.save(revivedPlacement);
            return;
        }
        StorageBlobPlacementEntity placementEntity = StorageBlobPlacementEntity.builder()
                .setBlobId(blobId)
                .setBackendName(backendName)
                .setObjectKey(objectKey)
                .setCreateTime(now)
                .setUpdateTime(now)
                .setDeleted(false)
                .build();
        try {
            storageBlobPlacementRepository.save(placementEntity);
        } catch (DataIntegrityViolationException exception) {
            StorageBlobPlacementEntity conflictedPlacement = storageBlobPlacementRepository
                    .findAnyByBlobIdAndBackendName(blobId, backendName)
                    .orElse(null);
            if (conflictedPlacement != null) {
                StorageBlobPlacementEntity updatedPlacement = conflictedPlacement.toBuilder()
                        .setObjectKey(objectKey)
                        .setDeleted(false)
                        .setUpdateTime(now)
                        .build();
                storageBlobPlacementRepository.save(updatedPlacement);
                return;
            }
            throw exception;
        }
    }

    private String newBlobId() {
        return resourceIdGenerator.nextId(StorageResourceKind.INSTANCE);
    }
}
