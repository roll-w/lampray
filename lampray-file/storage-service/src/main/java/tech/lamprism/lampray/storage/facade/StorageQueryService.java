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

import com.google.common.primitives.Ints;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.metamodel.SingularAttribute;
import tech.lamprism.lampray.storage.StorageBackendType;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageUploadSessionState;
import tech.lamprism.lampray.storage.backend.BlobStoreRegistration;
import tech.lamprism.lampray.storage.backend.BlobStoreRegistry;
import tech.lamprism.lampray.storage.configuration.StorageBackendConfig;
import tech.lamprism.lampray.storage.configuration.StorageGroupBackend;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageTopology;
import tech.lamprism.lampray.storage.domain.StorageUploadSessionModel;
import tech.lamprism.lampray.storage.monitoring.StorageBackendTotals;
import tech.lamprism.lampray.storage.monitoring.StorageGroupTotals;
import tech.lamprism.lampray.storage.monitoring.StorageStatisticsEngine;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementRepository;
import tech.lamprism.lampray.storage.persistence.StorageBlobRepository;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity_;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity_;
import tech.lamprism.lampray.storage.query.StorageBackendView;
import tech.lamprism.lampray.storage.query.StorageBlobPlacementView;
import tech.lamprism.lampray.storage.query.StorageBlobView;
import tech.lamprism.lampray.storage.query.StorageFileDetails;
import tech.lamprism.lampray.storage.query.StorageFileView;
import tech.lamprism.lampray.storage.query.StorageGroupView;
import tech.lamprism.lampray.storage.query.StorageQueryProvider;
import tech.lamprism.lampray.storage.query.StorageSessionDetails;
import tech.lamprism.lampray.storage.query.StorageSessionView;
import tech.lamprism.lampray.storage.session.UploadSessionStatus;
import tech.rollw.common.web.DataErrorCode;
import tech.rollw.common.web.page.ImmutablePage;
import tech.rollw.common.web.page.Page;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author RollW
 */
@Service
public class StorageQueryService implements StorageQueryProvider {
    private final StorageFileRepository storageFileRepository;
    private final StorageBlobRepository storageBlobRepository;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final StorageTopology storageTopology;
    private final BlobStoreRegistry blobStoreRegistry;
    private final StorageStatisticsEngine storageStatisticsEngine;

    public StorageQueryService(StorageFileRepository storageFileRepository,
                               StorageBlobRepository storageBlobRepository,
                               StorageBlobPlacementRepository storageBlobPlacementRepository,
                               StorageUploadSessionRepository storageUploadSessionRepository,
                               StorageTopology storageTopology,
                               BlobStoreRegistry blobStoreRegistry,
                               StorageStatisticsEngine storageStatisticsEngine) {
        this.storageFileRepository = storageFileRepository;
        this.storageBlobRepository = storageBlobRepository;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.storageTopology = storageTopology;
        this.blobStoreRegistry = blobStoreRegistry;
        this.storageStatisticsEngine = storageStatisticsEngine;
    }

    @Override
    public Page<StorageFileView> listFiles(int page,
                                           int size,
                                           String groupName,
                                           Long ownerUserId,
                                           String fileName) {
        int normalizedSize = Ints.constrainToRange(size, 1, 200);
        int normalizedPage = Math.max(page, 1);
        var pageable = PageRequest.of(normalizedPage - 1, normalizedSize);
        var entityPage = storageFileRepository.findAll(pageable, fileSpecification(groupName, ownerUserId, fileName));
        List<StorageFileView> content = entityPage.getContent().stream()
                .map(this::toFileView)
                .toList();
        return ImmutablePage.of(normalizedPage, normalizedSize, entityPage.getTotalPages(), content);
    }

    @Override
    public StorageFileDetails getFile(String fileId) {
        StorageFileEntity fileEntity = requireFile(fileId);
        StorageBlobEntity blobEntity = storageBlobRepository.findById(fileEntity.getBlobId())
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST, "Blob not found: " + fileEntity.getBlobId()));
        List<StorageBlobPlacementView> placements = storageBlobPlacementRepository.findAllByBlobId(blobEntity.getBlobId()).stream()
                .map(this::toBlobPlacementView)
                .toList();
        return new StorageFileDetails(
                toFileView(fileEntity),
                new StorageBlobView(
                        blobEntity.getBlobId(),
                        blobEntity.getContentChecksum(),
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
        int normalizedSize = Ints.constrainToRange(size, 1, 200);
        int normalizedPage = Math.max(page, 1);
        OffsetDateTime now = OffsetDateTime.now();
        var pageable = PageRequest.of(normalizedPage - 1, normalizedSize);
        var sessionPage = storageUploadSessionRepository.findAll(pageable, sessionSpecification(state, ownerUserId, fileName, now));
        List<StorageSessionView> content = sessionPage.getData().stream()
                .map(StorageUploadSessionModel::from)
                .map(uploadSession -> toSessionView(uploadSession, now))
                .toList();
        return ImmutablePage.of(normalizedPage, normalizedSize, (int) sessionPage.getTotal(), content);
    }

    @Override
    public StorageSessionDetails getSession(String uploadId) {
        StorageUploadSessionModel uploadSession = storageUploadSessionRepository.findById(uploadId)
                .map(StorageUploadSessionModel::from)
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST, "Upload session not found: " + uploadId));
        StorageFileView file = storageFileRepository.findById(uploadSession.getFileId())
                .map(this::toFileView)
                .orElse(null);
        OffsetDateTime now = OffsetDateTime.now();
        return new StorageSessionDetails(
                uploadSession.getUploadId(),
                uploadSession.getFileId(),
                uploadSession.getGroupName(),
                uploadSession.getFileName(),
                uploadSession.getFileSize(),
                uploadSession.getMimeType(),
                uploadSession.getFileType(),
                uploadSession.getContentChecksum(),
                uploadSession.getOwnerUserId(),
                uploadSession.getPrimaryBackend(),
                uploadSession.getObjectKey(),
                uploadSession.getUploadMode(),
                uploadSession.trackedStateAt(now),
                uploadSession.getExpiresAt(),
                uploadSession.getCreateTime(),
                uploadSession.getUpdateTime(),
                file
        );
    }

    @Override
    public List<StorageBackendView> listBackends() {
        Map<String, BlobStoreRegistration> registrationMap = blobStoreRegistry.registrations().stream()
                .collect(Collectors.toMap(BlobStoreRegistration::getBackendName, registration -> registration, (left, right) -> left, LinkedHashMap::new));
        Map<String, StorageBackendTotals> backendTotals = storageStatisticsEngine.loadBackendTotals();
        Set<String> backendNames = new LinkedHashSet<>(storageTopology.getBackends().keySet());
        backendNames.addAll(registrationMap.keySet());
        backendNames.addAll(backendTotals.keySet());

        List<StorageBackendView> result = new ArrayList<>();
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
                    registration == null ? Set.of() : registration.getBlobStore().getCapabilities(),
                    registration == null ? Map.of() : registration.getGroupWeights(),
                    config == null ? null : config.getEndpoint(),
                    config == null ? null : config.getPublicEndpoint(),
                    config == null ? null : config.getRegion(),
                    config == null ? null : config.getBucket(),
                    config == null ? null : config.getRootPrefix(),
                    config == null ? null : config.getRootPath(),
                    config != null && config.getNativeChecksumEnabled(),
                    config != null && config.getPathStyleAccess(),
                    totals.getPrimaryBlobCount(),
                    totals.getPlacementCount(),
                    totals.getUniqueBytes(),
                    totals.getPhysicalBytes()
            ));
        }
        return result;
    }

    @Override
    public List<StorageGroupView> listGroups() {
        Map<String, StorageGroupTotals> groupTotals = storageStatisticsEngine.loadGroupTotals();
        Set<String> groupNames = new LinkedHashSet<>(storageTopology.getGroups().keySet());
        groupNames.addAll(groupTotals.keySet());
        List<StorageGroupView> result = new ArrayList<>();
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
                    totals.getFileCount(),
                    totals.getLogicalBytes(),
                    totals.getDistinctBlobCount(),
                    totals.getUniqueBytes()
            ));
        }
        return result;
    }

    private StorageFileEntity requireFile(String fileId) {
        return storageFileRepository.findById(fileId)
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST, "File not found: " + fileId));
    }

    private StorageFileView toFileView(StorageFileEntity entity) {
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

    private StorageBlobPlacementView toBlobPlacementView(StorageBlobPlacementEntity placement) {
        return new StorageBlobPlacementView(
                placement.getPlacementId(),
                placement.getBackendName(),
                placement.getObjectKey(),
                placement.getCreateTime(),
                placement.getUpdateTime()
        );
    }

    private StorageSessionView toSessionView(StorageUploadSessionModel uploadSession,
                                             OffsetDateTime now) {
        return new StorageSessionView(
                uploadSession.getUploadId(),
                uploadSession.getFileId(),
                uploadSession.getGroupName(),
                uploadSession.getFileName(),
                uploadSession.getOwnerUserId(),
                uploadSession.getPrimaryBackend(),
                uploadSession.getUploadMode(),
                uploadSession.trackedStateAt(now),
                uploadSession.getExpiresAt(),
                uploadSession.getCreateTime(),
                uploadSession.getUpdateTime()
        );
    }

    private Specification<StorageFileEntity> fileSpecification(String groupName,
                                                               Long ownerUserId,
                                                               String fileName) {
        return combine(
                eqIfPresent(StorageFileEntity_.groupName, StringUtils.trimToNull(groupName)),
                eqIfPresent(StorageFileEntity_.ownerUserId, ownerUserId),
                likeIfPresent(StorageFileEntity_.fileName, fileName)
        );
    }

    private Specification<StorageUploadSessionEntity> sessionSpecification(StorageUploadSessionState state,
                                                                           Long ownerUserId,
                                                                           String fileName,
                                                                           OffsetDateTime now) {
        return combine(
                trackedStateSpecification(state, now),
                eqIfPresent(StorageUploadSessionEntity_.ownerUserId, ownerUserId),
                likeIfPresent(StorageUploadSessionEntity_.fileName, fileName)
        );
    }

    private Specification<StorageUploadSessionEntity> trackedStateSpecification(StorageUploadSessionState state,
                                                                                OffsetDateTime now) {
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
    private static <T> Specification<T> combine(Specification<T>... specifications) {
        Specification<T> result = Specification.where(null);
        for (Specification<T> specification : specifications) {
            if (specification != null) {
                result = result.and(specification);
            }
        }
        return result;
    }

    private static <T, V> Specification<T> eqIfPresent(SingularAttribute<T, V> attribute,
                                                       V value) {
        if (value == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(attribute), value);
    }

    private static <T> Specification<T> likeIfPresent(SingularAttribute<T, String> attribute,
                                                      String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String normalized = "%" + value.trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get(attribute)), normalized);
    }
}
