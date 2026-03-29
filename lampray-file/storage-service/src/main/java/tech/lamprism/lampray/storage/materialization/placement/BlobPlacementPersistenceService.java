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

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tech.lamprism.lampray.common.data.ResourceIdGenerator;
import tech.lamprism.lampray.storage.StorageResourceKind;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementRepository;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
@Service
public class BlobPlacementPersistenceService {
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final ResourceIdGenerator resourceIdGenerator;

    public BlobPlacementPersistenceService(StorageBlobPlacementRepository storageBlobPlacementRepository,
                                           ResourceIdGenerator resourceIdGenerator) {
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.resourceIdGenerator = resourceIdGenerator;
    }

    public void persistIfAbsent(String blobId,
                                String backendName,
                                String objectKey,
                                OffsetDateTime now) {
        if (storageBlobPlacementRepository.findByBlobIdAndBackendName(blobId, backendName).isPresent()) {
            return;
        }
        StorageBlobPlacementEntity placementEntity = StorageBlobPlacementEntity.builder()
                .setPlacementId(resourceIdGenerator.nextId(StorageResourceKind.INSTANCE))
                .setBlobId(blobId)
                .setBackendName(backendName)
                .setObjectKey(objectKey)
                .setCreateTime(now)
                .setUpdateTime(now)
                .build();
        try {
            storageBlobPlacementRepository.save(placementEntity);
        } catch (DataIntegrityViolationException exception) {
            if (storageBlobPlacementRepository.findByBlobIdAndBackendName(blobId, backendName).isPresent()) {
                return;
            }
            throw exception;
        }
    }
}
