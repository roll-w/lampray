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

package tech.lamprism.lampray.storage.facade;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tech.lamprism.lampray.storage.StorageBackendType;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageUploadSessionState;
import tech.lamprism.lampray.storage.monitoring.StorageBackendTotals;
import tech.lamprism.lampray.storage.monitoring.StorageGroupTotals;
import tech.lamprism.lampray.storage.monitoring.StorageStatisticsEngine;
import tech.lamprism.lampray.storage.query.StorageBackendView;
import tech.lamprism.lampray.storage.query.StorageBlobPlacementView;
import tech.lamprism.lampray.storage.query.StorageBlobView;
import tech.lamprism.lampray.storage.query.StorageFileDetails;
import tech.lamprism.lampray.storage.query.StorageFileView;
import tech.lamprism.lampray.storage.query.StorageGroupView;
import tech.lamprism.lampray.storage.query.StorageQueryProvider;
import tech.lamprism.lampray.storage.query.StorageSessionDetails;
import tech.lamprism.lampray.storage.query.StorageSessionView;
import tech.lamprism.lampray.storage.backend.BlobStoreRegistration;
import tech.lamprism.lampray.storage.backend.BlobStoreRegistry;
import tech.lamprism.lampray.storage.configuration.StorageBackendConfig;
import tech.lamprism.lampray.storage.configuration.StorageGroupBackend;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageTopology;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementRepository;
import tech.lamprism.lampray.storage.persistence.StorageBlobRepository;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity_;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity_;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.persistence.UploadSessionStatus;
import tech.lamprism.lampray.storage.session.StorageUploadSessionStates;
import tech.rollw.common.web.DataErrorCode;
import tech.rollw.common.web.page.ImmutablePage;
import tech.rollw.common.web.page.Page;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StorageQueryService implements StorageQueryProvider {
    private final StorageTopology storageTopology;
    private final BlobStoreRegistry blobStoreRegistry;
    private final StorageFileRepository storageFileRepository;
    private final StorageBlobRepository storageBlobRepository;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final StorageStatisticsEngine storageStatisticsEngine;

    public StorageQueryService(StorageTopology storageTopology,
                               BlobStoreRegistry blobStoreRegistry,
                               StorageFileRepository storageFileRepository,
                               StorageBlobRepository storageBlobRepository,
                               StorageBlobPlacementRepository storageBlobPlacementRepository,
                               StorageUploadSessionRepository storageUploadSessionRepository,
                               StorageStatisticsEngine storageStatisticsEngine) {
        this.storageTopology = storageTopology;
        this.blobStoreRegistry = blobStoreRegistry;
        this.storageFileRepository = storageFileRepository;
        this.storageBlobRepository = storageBlobRepository;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.storageStatisticsEngine = storageStatisticsEngine;
    }

    @Override
    public Page<StorageFileView> listFiles(int page,
                                           int size,
                                           String groupName,
                                           Long ownerUserId,
                                           String fileName) {
        var pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
        var entityPage = storageFileRepository.findAll(createFileSpecification(groupName, ownerUserId, fileName), pageable);
        List<StorageFileView> content = entityPage.getContent().stream()
                .map(this::toFileSummary)
                .toList();
        return ImmutablePage.of(page, normalizeSize(size), entityPage.getTotalPages(), content);
    }

    @Override
    public StorageFileDetails getFile(String fileId) {
        StorageFileEntity fileEntity = requireFile(fileId);
        StorageBlobEntity blobEntity = storageBlobRepository.findById(fileEntity.getBlobId())
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST, "Blob not found: " + fileEntity.getBlobId()));
        List<StorageBlobPlacementView> placements = storageBlobPlacementRepository.findAllByBlobId(blobEntity.getBlobId()).stream()
                .map(this::toPlacementView)
                .toList();
        return new StorageFileDetails(
                toFileSummary(fileEntity),
                new StorageBlobView(
                        blobEntity.getBlobId(),
                        blobEntity.getChecksumSha256(),
                        blobEntity.getFileSize(),
                        blobEntity.getMimeType(),
                        blobEntity.getFileType(),
                        blobEntity.getPrimaryBackend(),
                        blobEntity.getPrimaryObjectKey(),
                        blobEntity.getCreateTime(),
                        blobEntity.getUpdateTime(),
                        placements
                )
        );
    }

    @Override
    public Page<StorageSessionView> listSessions(int page,
                                                  int size,
                                                  StorageUploadSessionState state,
                                                  Long ownerUserId,
                                                  String fileName) {
        int normalizedSize = normalizeSize(size);
        int normalizedPage = Math.max(page, 1);
        var now = java.time.OffsetDateTime.now();
        var pageable = PageRequest.of(normalizePage(normalizedPage), normalizedSize);
        var entityPage = storageUploadSessionRepository.findAll(pageable, createSessionSpecification(state, ownerUserId, fileName, now));
        List<StorageSessionView> content = entityPage.getContent().stream()
                .map(entity -> toSessionSummary(entity, now))
                .toList();
        return ImmutablePage.of(normalizedPage, normalizedSize, entityPage.getTotalPages(), content);
    }

    @Override
    public StorageSessionDetails getSession(String uploadId) {
        StorageUploadSessionEntity entity = storageUploadSessionRepository.findById(uploadId)
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST, "Upload session not found: " + uploadId));
        StorageFileView file = storageFileRepository.findById(entity.getFileId())
                .map(this::toFileSummary)
                .orElse(null);
        var now = java.time.OffsetDateTime.now();
        return new StorageSessionDetails(
                entity.getUploadId(),
                entity.getFileId(),
                entity.getGroupName(),
                entity.getFileName(),
                entity.getFileSize(),
                entity.getMimeType(),
                entity.getFileType(),
                entity.getChecksumSha256(),
                entity.getOwnerUserId(),
                entity.getPrimaryBackend(),
                entity.getObjectKey(),
                entity.getUploadMode(),
                StorageUploadSessionStates.resolveTrackedState(entity, now),
                entity.getExpiresAt(),
                entity.getCreateTime(),
                entity.getUpdateTime(),
                file
        );
    }

    @Override
    public List<StorageBackendView> listBackends() {
        Map<String, BlobStoreRegistration> registrationMap = blobStoreRegistry.registrations().stream()
                .collect(Collectors.toMap(BlobStoreRegistration::backendName, registration -> registration, (left, right) -> left, LinkedHashMap::new));
        Map<String, StorageBackendTotals> backendTotals = storageStatisticsEngine.loadBackendTotals();
        Set<String> backendNames = new java.util.LinkedHashSet<>(storageTopology.getBackends().keySet());
        backendNames.addAll(registrationMap.keySet());
        backendNames.addAll(backendTotals.keySet());

        List<StorageBackendView> result = new java.util.ArrayList<>();
        for (String backendName : backendNames) {
            StorageBackendConfig config = storageTopology.getBackends().get(backendName);
            BlobStoreRegistration registration = registrationMap.get(backendName);
            StorageBackendType backendType = config != null ? config.getType() : null;
            StorageBackendTotals totals = backendTotals.getOrDefault(
                    backendName,
                    new StorageBackendTotals(0L, 0L, 0L, 0L)
            );
            result.add(new StorageBackendView(
                    backendName,
                    backendType,
                    registration != null,
                    registration == null ? Set.of() : registration.blobStore().getCapabilities(),
                    registration == null ? Map.of() : registration.groupWeights(),
                    config == null ? null : config.getEndpoint(),
                    config == null ? null : config.getPublicEndpoint(),
                    config == null ? null : config.getRegion(),
                    config == null ? null : config.getBucket(),
                    config == null ? null : config.getRootPrefix(),
                    config == null ? null : config.getRootPath(),
                    config != null && config.getNativeChecksumEnabled(),
                    config != null && config.getPathStyleAccess(),
                    totals.primaryBlobCount(),
                    totals.placementCount(),
                    totals.uniqueBytes(),
                    totals.physicalBytes()
            ));
        }
        return result;
    }

    @Override
    public List<StorageGroupView> listGroups() {
        Map<String, StorageGroupTotals> groupTotals = storageStatisticsEngine.loadGroupTotals();
        Set<String> groupNames = new java.util.LinkedHashSet<>(storageTopology.getGroups().keySet());
        groupNames.addAll(groupTotals.keySet());
        List<StorageGroupView> result = new java.util.ArrayList<>();
        for (String groupName : groupNames) {
            StorageGroupConfig groupConfig = storageTopology.getGroups().get(groupName);
            StorageGroupTotals totals = groupTotals.getOrDefault(
                    groupName,
                    new StorageGroupTotals(0L, 0L, 0L, 0L)
            );
            result.add(new StorageGroupView(
                    groupName,
                    groupConfig == null ? null : groupConfig.getVisibility(),
                    groupConfig == null ? null : groupConfig.getDownloadPolicy(),
                    groupConfig == null ? null : groupConfig.getPlacementMode(),
                    groupConfig == null ? null : groupConfig.getLoadBalanceMode(),
                    groupConfig == null ? null : groupConfig.getMaxSizeBytes(),
                    groupConfig == null ? Set.of() : groupConfig.getAllowedFileTypes(),
                    groupConfig == null ? List.of() : groupConfig.getBackends().stream().map(StorageGroupBackend::getBackendName).toList(),
                    totals.fileCount(),
                    totals.logicalBytes(),
                    totals.distinctBlobCount(),
                    totals.uniqueBytes()
            ));
        }
        return result;
    }

    private Specification<StorageFileEntity> createFileSpecification(String groupName,
                                                                     Long ownerUserId,
                                                                     String fileName) {
        return combine(
                eqIfPresent(StorageFileEntity_.groupName, groupName),
                eqIfPresent(StorageFileEntity_.ownerUserId, ownerUserId),
                likeIfPresent(StorageFileEntity_.fileName, fileName)
        );
    }

    private Specification<StorageUploadSessionEntity> createSessionSpecification(StorageUploadSessionState state,
                                                                                 Long ownerUserId,
                                                                                 String fileName,
                                                                                 java.time.OffsetDateTime now) {
        return combine(
                trackedStateSpec(state, now),
                eqIfPresent(StorageUploadSessionEntity_.ownerUserId, ownerUserId),
                likeIfPresent(StorageUploadSessionEntity_.fileName, fileName)
        );
    }

    private Specification<StorageUploadSessionEntity> trackedStateSpec(StorageUploadSessionState state,
                                                                      java.time.OffsetDateTime now) {
        if (state == null) {
            return null;
        }
        return switch (state) {
            case PENDING -> (root, query, criteriaBuilder) -> criteriaBuilder.and(
                    criteriaBuilder.equal(root.get(StorageUploadSessionEntity_.status), UploadSessionStatus.PENDING),
                    criteriaBuilder.greaterThanOrEqualTo(root.get(StorageUploadSessionEntity_.expiresAt), now)
            );
            case COMPLETED -> (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get(StorageUploadSessionEntity_.status), UploadSessionStatus.COMPLETED);
            case EXPIRED -> (root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.equal(root.get(StorageUploadSessionEntity_.status), UploadSessionStatus.EXPIRED),
                    criteriaBuilder.and(
                            criteriaBuilder.equal(root.get(StorageUploadSessionEntity_.status), UploadSessionStatus.PENDING),
                            criteriaBuilder.lessThan(root.get(StorageUploadSessionEntity_.expiresAt), now)
                    )
            );
        };
    }

    @SafeVarargs
    private <T> Specification<T> combine(Specification<T>... specifications) {
        Specification<T> result = Specification.where(null);
        for (Specification<T> specification : specifications) {
            if (specification != null) {
                result = result.and(specification);
            }
        }
        return result;
    }

    private <T, V> Specification<T> eqIfPresent(jakarta.persistence.metamodel.SingularAttribute<T, V> attribute,
                                                V value) {
        if (value == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(attribute), value);
    }

    private <T> Specification<T> likeIfPresent(jakarta.persistence.metamodel.SingularAttribute<T, String> attribute,
                                               String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = "%" + value.trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get(attribute)), normalized);
    }

    private int normalizePage(int page) {
        return Math.max(page - 1, 0);
    }

    private int normalizeSize(int size) {
        return Math.max(1, Math.min(size, 200));
    }

    private StorageFileView toFileSummary(StorageFileEntity entity) {
        return new StorageFileView(
                entity.getFileId(),
                entity.getBlobId(),
                entity.getGroupName(),
                entity.getOwnerUserId(),
                entity.getFileName(),
                entity.getFileSize(),
                entity.getMimeType(),
                entity.getFileType(),
                entity.getVisibility(),
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }

    private StorageBlobPlacementView toPlacementView(StorageBlobPlacementEntity placement) {
        return new StorageBlobPlacementView(
                placement.getPlacementId(),
                placement.getBackendName(),
                placement.getObjectKey(),
                placement.getCreateTime(),
                placement.getUpdateTime()
        );
    }

    private StorageSessionView toSessionSummary(StorageUploadSessionEntity entity,
                                                java.time.OffsetDateTime now) {
        StorageUploadSessionState trackedState = StorageUploadSessionStates.resolveTrackedState(entity, now);
        return new StorageSessionView(
                entity.getUploadId(),
                entity.getFileId(),
                entity.getGroupName(),
                entity.getFileName(),
                entity.getOwnerUserId(),
                entity.getPrimaryBackend(),
                entity.getUploadMode(),
                trackedState,
                entity.getExpiresAt(),
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }

    private StorageFileEntity requireFile(String fileId) {
        return storageFileRepository.findById(fileId)
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST, "File not found: " + fileId));
    }

}
