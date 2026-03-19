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

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tech.lamprism.lampray.common.data.ResourceIdGenerator;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageResourceKind;
import tech.lamprism.lampray.storage.configuration.StorageGroupPlacementMode;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementRepository;
import tech.lamprism.lampray.storage.persistence.StorageBlobRepository;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.store.BlobObject;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.store.BlobStoreRegistry;
import tech.lamprism.lampray.storage.store.BlobWriteRequest;
import tech.rollw.common.web.DataErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Encapsulates blob placement materialization and persistence so the provider
 * only orchestrates the upload flow.
 *
 * @author RollW
 */
@Service
public class DefaultStorageBlobMaterializationService implements StorageBlobMaterializationService, BlobPlacementWriter {
    private final StorageRuntimeConfig runtimeSettings;
    private final BlobStoreRegistry blobStoreRegistry;
    private final StorageBlobRepository storageBlobRepository;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final BlobObjectKeyFactory blobObjectKeyFactory;
    private final ResourceIdGenerator resourceIdGenerator;

    public DefaultStorageBlobMaterializationService(StorageRuntimeConfig runtimeSettings,
                                                    BlobStoreRegistry blobStoreRegistry,
                                                    StorageBlobRepository storageBlobRepository,
                                                    StorageBlobPlacementRepository storageBlobPlacementRepository,
                                                    BlobObjectKeyFactory blobObjectKeyFactory,
                                                    ResourceIdGenerator resourceIdGenerator) {
        this.runtimeSettings = runtimeSettings;
        this.blobStoreRegistry = blobStoreRegistry;
        this.storageBlobRepository = storageBlobRepository;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.blobObjectKeyFactory = blobObjectKeyFactory;
        this.resourceIdGenerator = resourceIdGenerator;
    }

    @Override
    public PreparedBlobMaterialization prepareBlobMaterialization(BlobMaterializationRequest request) throws IOException {
        StorageWritePlan writePlan = request.writePlan();
        BlobMaterializationSource source = request.source();
        Optional<StorageBlobEntity> existingBlob = runtimeSettings.deduplicationEnabled()
                ? storageBlobRepository.findByChecksumSha256(request.checksum())
                : Optional.empty();
        if (existingBlob.isPresent()) {
            StorageBlobEntity blobEntity = existingBlob.get();
            return PreparedBlobMaterialization.existing(
                    blobEntity,
                    request.size(),
                    ensureRequiredPlacements(
                            blobEntity,
                            request
                    )
            );
        }

        String primaryObjectKey = source.resolvePrimaryObjectKey(blobObjectKeyFactory, request.checksum());
        Map<String, String> materializedPlacements = new LinkedHashMap<>();
        try {
            source.materializePrimary(this, request, primaryObjectKey);
            materializedPlacements.put(request.primaryBackend(), primaryObjectKey);

            if (writePlan.groupSettings().getPlacementMode() == StorageGroupPlacementMode.MIRROR) {
                materializeMirrorPlacements(materializedPlacements, request, primaryObjectKey);
            }
        } catch (IOException | RuntimeException exception) {
            cleanupMaterializedPlacements(
                    materializedPlacements,
                    source.protectsPrimaryPlacement() ? request.primaryBackend() : null,
                    source.protectsPrimaryPlacement() ? primaryObjectKey : null
            );
            throw exception;
        }
        return PreparedBlobMaterialization.newBlob(
                request.checksum(),
                request.size(),
                request.mimeType(),
                request.fileType(),
                request.primaryBackend(),
                primaryObjectKey,
                materializedPlacements
        );
    }

    @Override
    public StorageBlobEntity persistBlobMaterialization(PreparedBlobMaterialization preparedBlob) {
        StorageBlobEntity blobEntity = preparedBlob.existingBlob();
        if (blobEntity == null) {
            OffsetDateTime now = OffsetDateTime.now();
            StorageBlobEntity candidate = StorageBlobEntity.builder()
                    .setBlobId(newBlobId())
                    .setChecksumSha256(preparedBlob.checksum())
                    .setFileSize(preparedBlob.size())
                    .setMimeType(preparedBlob.mimeType())
                    .setFileType(preparedBlob.fileType())
                    .setPrimaryBackend(preparedBlob.primaryBackend())
                    .setPrimaryObjectKey(preparedBlob.primaryObjectKey())
                    .setCreateTime(now)
                    .setUpdateTime(now)
                    .build();
            try {
                blobEntity = storageBlobRepository.save(candidate);
            } catch (DataIntegrityViolationException exception) {
                blobEntity = storageBlobRepository.findByChecksumSha256(preparedBlob.checksum())
                        .orElseThrow(() -> exception);
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (Map.Entry<String, String> entry : preparedBlob.placementsToPersist().entrySet()) {
            savePlacement(blobEntity.getBlobId(), entry.getKey(), entry.getValue(), now);
        }
        return blobEntity;
    }

    private Map<String, String> ensureRequiredPlacements(StorageBlobEntity blobEntity,
                                                         BlobMaterializationRequest request) throws IOException {
        Map<String, String> placementsToPersist = new LinkedHashMap<>();
        Set<String> requiredBackends = new LinkedHashSet<>();
        requiredBackends.add(request.primaryBackend());
        if (request.writePlan().groupSettings().getPlacementMode() == StorageGroupPlacementMode.MIRROR) {
            requiredBackends.addAll(request.writePlan().mirrorBackends());
        }

        BlobMaterializationSource source = request.source();
        String sourceBackend = source.resolveSourceBackend(blobEntity, request);
        String sourceObjectKey = source.resolveSourceObjectKey(blobEntity, request, blobEntity.getPrimaryObjectKey());

        try {
            for (String backendName : requiredBackends) {
                if (storageBlobPlacementRepository.findByBlobIdAndBackendName(blobEntity.getBlobId(), backendName).isPresent()) {
                    continue;
                }

                String objectKey = backendName.equals(sourceBackend)
                        ? sourceObjectKey
                        : blobObjectKeyFactory.createKey(request.checksum());
                materializePlacement(
                        placementsToPersist,
                        request,
                        backendName,
                        objectKey,
                        sourceBackend,
                        sourceObjectKey,
                        source
                );
            }
        } catch (IOException | RuntimeException exception) {
            cleanupMaterializedPlacements(placementsToPersist, sourceBackend, sourceObjectKey);
            throw exception;
        }
        return placementsToPersist;
    }

    private void materializeMirrorPlacements(Map<String, String> materializedPlacements,
                                             BlobMaterializationRequest request,
                                             String primaryObjectKey) throws IOException {
        for (String mirrorBackend : request.writePlan().mirrorBackends()) {
            if (materializedPlacements.containsKey(mirrorBackend)) {
                continue;
            }
            materializePlacement(
                    materializedPlacements,
                    request,
                    mirrorBackend,
                    blobObjectKeyFactory.createKey(request.checksum()),
                    request.primaryBackend(),
                    primaryObjectKey,
                    request.source()
                );
        }
    }

    private void materializePlacement(Map<String, String> materializedPlacements,
                                      BlobMaterializationRequest request,
                                      String backendName,
                                      String objectKey,
                                      String sourceBackend,
                                      String sourceObjectKey,
                                      BlobMaterializationSource source) throws IOException {
        if (backendName.equals(sourceBackend)) {
            materializedPlacements.put(backendName, objectKey);
            return;
        }
        source.materializeReplica(this, request, backendName, objectKey, sourceBackend, sourceObjectKey);
        materializedPlacements.put(backendName, objectKey);
    }

    @Override
    public void replicateBetweenBackends(String sourceBackend,
                                         String sourceObjectKey,
                                         String targetBackend,
                                         String targetObjectKey,
                                         long size,
                                         String mimeType,
                                         String checksum) throws IOException {
        Path tempFile = Files.createTempFile("lampray-replica-", ".bin");
        try {
            try (OutputStream outputStream = Files.newOutputStream(tempFile, StandardOpenOption.TRUNCATE_EXISTING)) {
                requireBlobStore(sourceBackend).openDownload(sourceObjectKey)
                        .transferTo(outputStream);
            }
            putTempToBackend(targetBackend, targetObjectKey, tempFile, size, mimeType, checksum);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Override
    public void putTempToBackend(String backendName,
                                 String objectKey,
                                 Path tempPath,
                                 long size,
                                 String mimeType,
                                 String checksumSha256) throws IOException {
        try (InputStream inputStream = Files.newInputStream(Objects.requireNonNull(tempPath))) {
            requireBlobStore(backendName).store(
                    new BlobWriteRequest(
                            objectKey,
                            size,
                            mimeType,
                            buildBlobMetadata(checksumSha256),
                            checksumSha256
                    ),
                    inputStream
            );
        }
    }

    private void savePlacement(String blobId,
                               String backendName,
                               String objectKey,
                               OffsetDateTime now) {
        if (storageBlobPlacementRepository.findByBlobIdAndBackendName(blobId, backendName).isPresent()) {
            return;
        }
        StorageBlobPlacementEntity placementEntity = StorageBlobPlacementEntity.builder()
                .setPlacementId(newPlacementId())
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

    private BlobStore requireBlobStore(String backendName) {
        return blobStoreRegistry.find(backendName)
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "Storage backend is not available: " + backendName));
    }

    private Map<String, String> buildBlobMetadata(String checksumSha256) {
        if (checksumSha256 == null) {
            return Map.of();
        }
        return Map.of("checksum-sha256", checksumSha256);
    }

    private void cleanupMaterializedPlacements(Map<String, String> placements,
                                               String protectedBackend,
                                               String protectedObjectKey) {
        for (Map.Entry<String, String> entry : placements.entrySet()) {
            boolean isProtected = protectedBackend != null
                    && protectedBackend.equals(entry.getKey())
                    && Objects.equals(protectedObjectKey, entry.getValue());
            if (isProtected) {
                continue;
            }
            try {
                requireBlobStore(entry.getKey()).delete(entry.getValue());
            } catch (IOException ignored) {
            }
        }
    }

    private String newBlobId() {
        return resourceIdGenerator.nextId(StorageResourceKind.INSTANCE);
    }

    private String newPlacementId() {
        return resourceIdGenerator.nextId(StorageResourceKind.INSTANCE);
    }
}
