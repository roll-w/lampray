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

package tech.lamprism.lampray.storage.session;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import tech.lamprism.lampray.common.data.ResourceIdGenerator;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageAccessRequest;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageResourceKind;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.StorageUploadSession;
import tech.lamprism.lampray.storage.StorageUploadSessionDetails;
import tech.lamprism.lampray.storage.StorageUploadSessionState;
import tech.lamprism.lampray.storage.backend.BlobStoreRegistry;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.materialization.BlobObjectKeyFactory;
import tech.lamprism.lampray.storage.monitoring.StorageTrafficRecorder;
import tech.lamprism.lampray.storage.policy.ChecksumNormalizer;
import tech.lamprism.lampray.storage.policy.FileNameSanitizer;
import tech.lamprism.lampray.storage.policy.StorageContentPolicy;
import tech.lamprism.lampray.storage.policy.StorageTransferPolicy;
import tech.lamprism.lampray.storage.policy.UploadRequestValidator;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.persistence.UploadSessionStatus;
import tech.lamprism.lampray.storage.routing.StorageGroupRouter;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.store.BlobWriteRequest;
import tech.rollw.common.web.AuthErrorCode;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.DataErrorCode;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class DefaultStorageUploadSessionService implements StorageUploadSessionService {
    private final StorageRuntimeConfig runtimeSettings;
    private final BlobStoreRegistry blobStoreRegistry;
    private final StorageFileRepository storageFileRepository;
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final ResourceIdGenerator resourceIdGenerator;
    private final StorageGroupRouter storageGroupRouter;
    private final StorageTransferPolicy storageTransferPolicy;
    private final StorageContentPolicy storageContentPolicy;
    private final BlobObjectKeyFactory blobObjectKeyFactory;
    private final FileNameSanitizer fileNameSanitizer;
    private final ChecksumNormalizer checksumNormalizer;
    private final UploadRequestValidator uploadRequestValidator;
    private final StorageTrafficRecorder storageTrafficRecorder;
    private final TransactionTemplate transactionTemplate;

    public DefaultStorageUploadSessionService(StorageRuntimeConfig runtimeSettings,
                                             BlobStoreRegistry blobStoreRegistry,
                                             StorageFileRepository storageFileRepository,
                                             StorageUploadSessionRepository storageUploadSessionRepository,
                                             ResourceIdGenerator resourceIdGenerator,
                                             StorageGroupRouter storageGroupRouter,
                                             StorageTransferPolicy storageTransferPolicy,
                                             StorageContentPolicy storageContentPolicy,
                                             BlobObjectKeyFactory blobObjectKeyFactory,
                                             FileNameSanitizer fileNameSanitizer,
                                             ChecksumNormalizer checksumNormalizer,
                                             UploadRequestValidator uploadRequestValidator,
                                             StorageTrafficRecorder storageTrafficRecorder,
                                             PlatformTransactionManager transactionManager) {
        this.runtimeSettings = runtimeSettings;
        this.blobStoreRegistry = blobStoreRegistry;
        this.storageFileRepository = storageFileRepository;
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.resourceIdGenerator = resourceIdGenerator;
        this.storageGroupRouter = storageGroupRouter;
        this.storageTransferPolicy = storageTransferPolicy;
        this.storageContentPolicy = storageContentPolicy;
        this.blobObjectKeyFactory = blobObjectKeyFactory;
        this.fileNameSanitizer = fileNameSanitizer;
        this.checksumNormalizer = checksumNormalizer;
        this.uploadRequestValidator = uploadRequestValidator;
        this.storageTrafficRecorder = storageTrafficRecorder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public StorageUploadSession createUploadSession(StorageUploadRequest request,
                                                    Long userId) throws IOException {
        String groupName = resolveGroupName(request.getGroupName());
        StorageWritePlan writePlan = selectWritePlan(groupName);
        StorageGroupConfig groupSettings = writePlan.getGroupSettings();
        String fileName = fileNameSanitizer.normalize(request.getFileName());
        String mimeType = storageContentPolicy.requireMimeType(request.getMimeType());
        FileType fileType = storageContentPolicy.resolveFileType(mimeType);
        uploadRequestValidator.validate(request, groupSettings, fileType);

        String checksum = checksumNormalizer.normalize(request.getChecksumSha256());
        String uploadId = newId();
        String fileId = newId();
        String primaryBackend = writePlan.getPrimaryBackend();
        BlobStore primaryBlobStore = requireBlobStore(primaryBackend);
        StorageUploadMode uploadMode = storageTransferPolicy.resolveUploadMode(request, checksum, primaryBlobStore);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusSeconds(runtimeSettings.getPendingUploadExpireSeconds());

        StorageAccessRequest directRequest = null;
        String objectKey = null;
        if (uploadMode == StorageUploadMode.DIRECT) {
            long declaredSize = Objects.requireNonNull(request.getSize(), "Direct uploads require a declared size.");
            objectKey = blobObjectKeyFactory.createKey(Objects.requireNonNull(checksum));
            directRequest = primaryBlobStore.createDirectUpload(
                    new BlobWriteRequest(
                            objectKey,
                            declaredSize,
                            mimeType,
                            buildBlobMetadata(checksum),
                            checksum
                    ),
                    Duration.ofSeconds(runtimeSettings.getDirectAccessTtlSeconds())
            );
            storageTrafficRecorder.recordDirectUploadRequest(groupName, primaryBackend, declaredSize);
        }

        StorageUploadSessionEntity uploadSessionEntity = StorageUploadSessionEntity.builder()
                .setUploadId(uploadId)
                .setFileId(fileId)
                .setGroupName(groupName)
                .setFileName(fileName)
                .setFileSize(request.getSize())
                .setMimeType(mimeType)
                .setFileType(fileType)
                .setChecksumSha256(checksum)
                .setOwnerUserId(userId)
                .setPrimaryBackend(primaryBackend)
                .setObjectKey(objectKey)
                .setUploadMode(uploadMode)
                .setStatus(UploadSessionStatus.PENDING)
                .setExpiresAt(expiresAt)
                .setCreateTime(now)
                .setUpdateTime(now)
                .build();
        transactionTemplate.executeWithoutResult(status -> storageUploadSessionRepository.save(uploadSessionEntity));

        return new StorageUploadSession(
                uploadId,
                uploadMode,
                fileName,
                groupName,
                fileId,
                directRequest,
                expiresAt
        );
    }

    @Override
    public StorageUploadSessionEntity requireUploadSession(String uploadId) {
        return storageUploadSessionRepository.findById(uploadId)
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "Upload session not found: " + uploadId));
    }

    @Override
    public StorageUploadSessionEntity requireActiveUploadSession(String uploadId,
                                                                 Long userId) {
        StorageUploadSessionEntity uploadSession = requireUploadSession(uploadId);
        if (uploadSession.getStatus() != UploadSessionStatus.PENDING) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session is not pending: " + uploadSession.getUploadId());
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (uploadSession.getExpiresAt().isBefore(now)) {
            transactionTemplate.executeWithoutResult(status -> expireUploadSession(uploadSession, now));
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session has expired: " + uploadSession.getUploadId());
        }
        ensureUploadSessionAuthorized(uploadSession, userId);
        return uploadSession;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public StorageUploadSessionDetails getUploadSession(String uploadId,
                                                        Long userId) {
        StorageUploadSessionEntity uploadSession = requireUploadSession(uploadId);
        ensureUploadSessionQueryable(uploadSession, userId);
        StorageUploadSessionState sessionState = resolveTrackedSessionState(uploadSession);
        FileStorage fileStorage = sessionState == StorageUploadSessionState.COMPLETED
                ? storageFileRepository.findById(uploadSession.getFileId()).orElseThrow(() -> new StorageException(
                DataErrorCode.ERROR_DATA_NOT_EXIST,
                "File not found: " + uploadSession.getFileId()
        )).lock()
                : null;
        return new StorageUploadSessionDetails(
                uploadSession.getUploadId(),
                uploadSession.getUploadMode(),
                uploadSession.getFileName(),
                uploadSession.getGroupName(),
                uploadSession.getFileId(),
                sessionState,
                uploadSession.getExpiresAt(),
                uploadSession.getCreateTime(),
                uploadSession.getUpdateTime(),
                fileStorage
        );
    }

    @Override
    public void expirePendingUploadSession(String uploadId) {
        transactionTemplate.executeWithoutResult(status -> storageUploadSessionRepository.findById(uploadId)
                .filter(uploadSession -> uploadSession.getStatus() == UploadSessionStatus.PENDING)
                .ifPresent(uploadSession -> expireUploadSession(uploadSession, OffsetDateTime.now())));
    }

    @Override
    public void markCompleted(StorageUploadSessionEntity uploadSession,
                              OffsetDateTime now) {
        uploadSession.setStatus(UploadSessionStatus.COMPLETED);
        uploadSession.setUpdateTime(now);
        storageUploadSessionRepository.save(uploadSession);
    }

    private StorageWritePlan selectWritePlan(String groupName) {
        try {
            return storageGroupRouter.selectWritePlan(groupName);
        } catch (IllegalStateException exception) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, exception.getMessage());
        }
    }

    private String resolveGroupName(String requestedGroupName) {
        if (!StringUtils.hasText(requestedGroupName)) {
            return runtimeSettings.getDefaultGroup();
        }
        return requestedGroupName.trim();
    }

    private BlobStore requireBlobStore(String backendName) {
        return blobStoreRegistry.find(backendName)
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "Storage backend is not available: " + backendName));
    }

    private void ensureUploadSessionAuthorized(StorageUploadSessionEntity uploadSession,
                                               Long userId) {
        if (uploadSession.getOwnerUserId() != null && !uploadSession.getOwnerUserId().equals(userId)) {
            throw new StorageException(AuthErrorCode.ERROR_UNAUTHORIZED_USE,
                    "You are not allowed to use this upload session.");
        }
    }

    private void ensureUploadSessionQueryable(StorageUploadSessionEntity uploadSession,
                                              Long userId) {
        if (userId == null || uploadSession.getOwnerUserId() == null || !uploadSession.getOwnerUserId().equals(userId)) {
            throw new StorageException(AuthErrorCode.ERROR_UNAUTHORIZED_USE,
                    "You are not allowed to query this upload session.");
        }
    }

    private void expireUploadSession(StorageUploadSessionEntity uploadSession,
                                     OffsetDateTime now) {
        uploadSession.setStatus(UploadSessionStatus.EXPIRED);
        uploadSession.setUpdateTime(now);
        storageUploadSessionRepository.save(uploadSession);
    }

    private StorageUploadSessionState resolveTrackedSessionState(StorageUploadSessionEntity uploadSession) {
        return StorageUploadSessionStates.resolveTrackedState(uploadSession, OffsetDateTime.now());
    }

    private Map<String, String> buildBlobMetadata(String checksumSha256) {
        if (checksumSha256 == null) {
            return Map.of();
        }
        return Map.of("checksum-sha256", checksumSha256);
    }

    private String newId() {
        return resourceIdGenerator.nextId(StorageResourceKind.INSTANCE);
    }
}
