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
import jakarta.persistence.metamodel.SingularAttribute;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageBackendType;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageUploadSessionState;
import tech.lamprism.lampray.storage.StorageVisibility;
import tech.lamprism.lampray.storage.backend.BlobStoreRegistration;
import tech.lamprism.lampray.storage.backend.BlobStoreRegistry;
import tech.lamprism.lampray.storage.configuration.StorageBackendConfig;
import tech.lamprism.lampray.storage.configuration.StorageGroupBackend;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageGroupDownloadPolicy;
import tech.lamprism.lampray.storage.configuration.StorageGroupLoadBalanceMode;
import tech.lamprism.lampray.storage.configuration.StorageGroupPlacementMode;
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
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity_;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
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
import tech.lamprism.lampray.storage.store.BlobStoreCapability;
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
        List<StorageFileView> content = entityPage.getData().stream()
                .map(this::toFileView)
                .toList();
        return ImmutablePage.of(normalizedPage, normalizedSize, (int) entityPage.getTotal(), content);
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
        PageRequest pageable = PageRequest.of(normalizedPage - 1, normalizedSize);
        Page<StorageUploadSessionEntity> sessionPage = storageUploadSessionRepository.findAll(pageable, sessionSpecification(state, ownerUserId, fileName, now));
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
        StorageFileView file = storageFileRepository.findActiveById(uploadSession.getFileId())
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
            StorageBackendType backendType = null;
            Set<BlobStoreCapability> capabilities = Set.of();
            Map<String, Integer> groupWeights = Map.of();
            String endpoint = null;
            String publicEndpoint = null;
            String region = null;
            String bucket = null;
            String rootPrefix = null;
            String rootPath = null;
            boolean nativeChecksumEnabled = false;
            boolean pathStyleAccess = false;
            if (registration != null) {
                capabilities = registration.getBlobStore().getCapabilities();
                groupWeights = registration.getGroupWeights();
            }
            if (config != null) {
                backendType = config.getType();
                endpoint = config.getEndpoint();
                publicEndpoint = config.getPublicEndpoint();
                region = config.getRegion();
                bucket = config.getBucket();
                rootPrefix = config.getRootPrefix();
                rootPath = config.getRootPath();
                nativeChecksumEnabled = config.getNativeChecksumEnabled();
                pathStyleAccess = config.getPathStyleAccess();
            }
            StorageBackendTotals totals = backendTotals.getOrDefault(
                    backendName,
                    new StorageBackendTotals(0L, 0L, 0L, 0L)
            );
            result.add(new StorageBackendView(
                    backendName,
                    backendType,
                    registration != null,
                    capabilities,
                    groupWeights,
                    endpoint,
                    publicEndpoint,
                    region,
                    bucket,
                    rootPrefix,
                    rootPath,
                    nativeChecksumEnabled,
                    pathStyleAccess,
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
            StorageVisibility visibility = null;
            StorageGroupDownloadPolicy downloadPolicy = null;
            StorageGroupPlacementMode placementMode = null;
            StorageGroupLoadBalanceMode loadBalanceMode = null;
            Long maxSizeBytes = null;
            Set<FileType> allowedFileTypes = Set.of();
            List<String> backends = List.of();
            if (groupConfig != null) {
                visibility = groupConfig.getVisibility();
                downloadPolicy = groupConfig.getDownloadPolicy();
                placementMode = groupConfig.getPlacementMode();
                loadBalanceMode = groupConfig.getLoadBalanceMode();
                maxSizeBytes = groupConfig.getMaxSizeBytes();
                allowedFileTypes = groupConfig.getAllowedFileTypes();
                backends = groupConfig.getBackends().stream().map(StorageGroupBackend::getBackendName).toList();
            }
            StorageGroupTotals totals = groupTotals.getOrDefault(
                    groupName,
                    new StorageGroupTotals(0L, 0L, 0L, 0L)
            );
            result.add(new StorageGroupView(
                    groupName,
                    visibility,
                    downloadPolicy,
                    placementMode,
                    loadBalanceMode,
                    maxSizeBytes,
                    allowedFileTypes,
                    backends,
                    totals.getFileCount(),
                    totals.getLogicalBytes(),
                    totals.getDistinctBlobCount(),
                    totals.getUniqueBytes()
            ));
        }
        return result;
    }

    private StorageFileEntity requireFile(String fileId) {
        return storageFileRepository.findActiveById(fileId)
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST, "File not found: " + fileId));
    }

    private StorageFileView toFileView(StorageFileEntity entity) {
        return entity.toFileView();
    }

    private StorageBlobPlacementView toBlobPlacementView(StorageBlobPlacementEntity placement) {
        return placement.toView();
    }

    private StorageSessionView toSessionView(StorageUploadSessionModel uploadSession,
                                             OffsetDateTime now) {
        return uploadSession.toSessionView(now);
    }

    private Specification<StorageFileEntity> fileSpecification(String groupName,
                                                               Long ownerUserId,
                                                               String fileName) {
        return combine(
                activeFileSpecification(),
                eqIfPresent(StorageFileEntity_.groupName, StringUtils.trimToNull(groupName)),
                eqIfPresent(StorageFileEntity_.ownerUserId, ownerUserId),
                likeIfPresent(StorageFileEntity_.fileName, fileName)
        );
    }

    private Specification<StorageFileEntity> activeFileSpecification() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get(StorageFileEntity_.deleted));
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
        Specification<T> result = Specification.unrestricted();
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
