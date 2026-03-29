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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageUploadSessionState;
import tech.lamprism.lampray.storage.query.StorageBlobPlacementView;
import tech.lamprism.lampray.storage.query.StorageBlobView;
import tech.lamprism.lampray.storage.query.StorageFileDetails;
import tech.lamprism.lampray.storage.query.StorageFileView;
import tech.lamprism.lampray.storage.query.StorageQueryProvider;
import tech.lamprism.lampray.storage.query.StorageSessionDetails;
import tech.lamprism.lampray.storage.query.StorageSessionView;
import tech.lamprism.lampray.storage.query.StorageBackendView;
import tech.lamprism.lampray.storage.query.StorageGroupView;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementRepository;
import tech.lamprism.lampray.storage.persistence.StorageBlobRepository;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.persistence.specification.StorageEntitySpecifications;
import tech.lamprism.lampray.storage.session.StorageUploadSessionStates;
import tech.rollw.common.web.DataErrorCode;
import tech.rollw.common.web.page.ImmutablePage;
import tech.rollw.common.web.page.Page;

import java.util.List;

/**
 * @author RollW
 */
@Service
public class StorageQueryService implements StorageQueryProvider {
    private final StorageFileRepository storageFileRepository;
    private final StorageBlobRepository storageBlobRepository;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final StorageTopologyQueryService storageTopologyQueryService;

    public StorageQueryService(StorageFileRepository storageFileRepository,
                               StorageBlobRepository storageBlobRepository,
                               StorageBlobPlacementRepository storageBlobPlacementRepository,
                               StorageUploadSessionRepository storageUploadSessionRepository,
                               StorageTopologyQueryService storageTopologyQueryService) {
        this.storageFileRepository = storageFileRepository;
        this.storageBlobRepository = storageBlobRepository;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.storageTopologyQueryService = storageTopologyQueryService;
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
        var entityPage = storageFileRepository.findAll(StorageEntitySpecifications.files(groupName, ownerUserId, fileName), pageable);
        List<StorageFileView> content = entityPage.getContent().stream()
                .map(this::toFileSummary)
                .toList();
        return ImmutablePage.of(normalizedPage, normalizedSize, entityPage.getTotalPages(), content);
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
        int normalizedSize = Ints.constrainToRange(size, 1, 200);
        int normalizedPage = Math.max(page, 1);
        var now = java.time.OffsetDateTime.now();
        var pageable = PageRequest.of(normalizedPage - 1, normalizedSize);
        var entityPage = storageUploadSessionRepository.findAll(pageable, StorageEntitySpecifications.sessions(state, ownerUserId, fileName, now));
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
        return storageTopologyQueryService.listBackends();
    }

    @Override
    public List<StorageGroupView> listGroups() {
        return storageTopologyQueryService.listGroups();
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
