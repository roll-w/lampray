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
import tech.lamprism.lampray.storage.materialization.placement.BlobPlacementPersistenceService;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobRepository;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * @author RollW
 */
@Service
public class BlobMaterializationPersistenceService {
    private final StorageBlobRepository storageBlobRepository;
    private final BlobPlacementPersistenceService blobPlacementPersistenceService;
    private final ResourceIdGenerator resourceIdGenerator;

    public BlobMaterializationPersistenceService(StorageBlobRepository storageBlobRepository,
                                                 BlobPlacementPersistenceService blobPlacementPersistenceService,
                                                 ResourceIdGenerator resourceIdGenerator) {
        this.storageBlobRepository = storageBlobRepository;
        this.blobPlacementPersistenceService = blobPlacementPersistenceService;
        this.resourceIdGenerator = resourceIdGenerator;
    }

    public StorageBlobEntity persist(PreparedBlobMaterialization preparedBlob) {
        StorageBlobEntity blobEntity = preparedBlob.getExistingBlob();
        if (blobEntity == null) {
            OffsetDateTime now = OffsetDateTime.now();
            StorageBlobEntity candidate = StorageBlobEntity.builder()
                    .setBlobId(newBlobId())
                    .setChecksumSha256(preparedBlob.getChecksum())
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
                blobEntity = storageBlobRepository.findByChecksumSha256(preparedBlob.getChecksum())
                        .orElseThrow(() -> exception);
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (Map.Entry<String, String> entry : preparedBlob.getPlacementsToPersist().entrySet()) {
            blobPlacementPersistenceService.persistIfAbsent(blobEntity.getBlobId(), entry.getKey(), entry.getValue(), now);
        }
        return blobEntity;
    }

    private String newBlobId() {
        return resourceIdGenerator.nextId(StorageResourceKind.INSTANCE);
    }
}
