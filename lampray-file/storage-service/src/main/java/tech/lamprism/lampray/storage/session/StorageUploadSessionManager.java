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
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.StorageUploadSession;
import tech.lamprism.lampray.storage.StorageUploadSessionDetails;
import tech.lamprism.lampray.storage.StorageUploadSessionState;
import tech.lamprism.lampray.storage.domain.StorageUploadSessionModel;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.session.workflow.CreateUploadSessionWorkflow;
import tech.lamprism.lampray.storage.session.workflow.CreateUploadSessionWorkflowContext;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.DataErrorCode;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * @author RollW
 */
@Service
@Transactional(readOnly = true)
public class StorageUploadSessionManager {
    private final CreateUploadSessionWorkflow createUploadSessionWorkflow;
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final StorageFileRepository storageFileRepository;
    private final TransactionTemplate expireTransactionTemplate;

    public StorageUploadSessionManager(CreateUploadSessionWorkflow createUploadSessionWorkflow,
                                       StorageUploadSessionRepository storageUploadSessionRepository,
                                       StorageFileRepository storageFileRepository,
                                       PlatformTransactionManager transactionManager) {
        this.createUploadSessionWorkflow = createUploadSessionWorkflow;
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.storageFileRepository = storageFileRepository;
        this.expireTransactionTemplate = new TransactionTemplate(transactionManager);
        this.expireTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public StorageUploadSession createUploadSession(StorageUploadRequest request,
                                                    Long userId) throws IOException {
        return createUploadSessionWorkflow.execute(new CreateUploadSessionWorkflowContext(request, userId));
    }

    public StorageUploadSessionModel requireUploadSession(String uploadId) {
        return storageUploadSessionRepository.findById(uploadId)
                .map(StorageUploadSessionModel::from)
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "Upload session not found: " + uploadId));
    }

    public StorageUploadSessionModel requireUploadSessionAuthorized(String uploadId,
                                                                    Long userId) {
        StorageUploadSessionModel uploadSession = requireUploadSession(uploadId);
        uploadSession.ensureAuthorized(userId);
        return uploadSession;
    }

    public StorageUploadSessionModel requireActiveUploadSession(String uploadId,
                                                                Long userId) {
        StorageUploadSessionModel uploadSession = requireUploadSessionAuthorized(uploadId, userId);
        if (uploadSession.getStatus() != UploadSessionStatus.PENDING) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session is not pending: " + uploadSession.getUploadId());
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (uploadSession.trackedStateAt(now) == StorageUploadSessionState.EXPIRED) {
            expire(uploadSession.getUploadId(), now);
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session has expired: " + uploadSession.getUploadId());
        }
        return uploadSession;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public StorageUploadSessionDetails getUploadSession(String uploadId,
                                                        Long userId) {
        StorageUploadSessionModel uploadSession = requireUploadSession(uploadId);
        uploadSession.ensureQueryable(userId);
        OffsetDateTime now = OffsetDateTime.now();
        FileStorage fileStorage = uploadSession.trackedStateAt(now) == StorageUploadSessionState.COMPLETED
                ? toFileStorage(requireStoredFile(uploadSession.getFileId()))
                : null;
        return new StorageUploadSessionDetails(
                uploadSession.getUploadId(),
                uploadSession.getUploadMode(),
                uploadSession.getFileName(),
                uploadSession.getGroupName(),
                uploadSession.getFileId(),
                uploadSession.trackedStateAt(now),
                uploadSession.getExpiresAt(),
                uploadSession.getCreateTime(),
                uploadSession.getUpdateTime(),
                fileStorage
        );
    }

    public void expirePendingUploadSession(String uploadId) {
        expire(uploadId, OffsetDateTime.now());
    }

    private void expire(String uploadId,
                        OffsetDateTime now) {
        expireTransactionTemplate.executeWithoutResult(status -> storageUploadSessionRepository.findById(uploadId)
                .filter(uploadSession -> uploadSession.getStatus() == UploadSessionStatus.PENDING)
                .ifPresent(uploadSession -> {
                    StorageUploadSessionModel.from(uploadSession).expire(now);
                    storageUploadSessionRepository.save(uploadSession);
                }));
    }

    private StorageFileEntity requireStoredFile(String fileId) {
        return storageFileRepository.findById(fileId)
                .orElseThrow(() -> new StorageException(
                        DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "File not found: " + fileId
                ));
    }

    private FileStorage toFileStorage(StorageFileEntity fileEntity) {
        return new FileStorage(
                fileEntity.getFileId(),
                fileEntity.getFileName(),
                fileEntity.getFileSize(),
                fileEntity.getMimeType(),
                fileEntity.getFileType(),
                fileEntity.getCreateTime()
        );
    }
}
