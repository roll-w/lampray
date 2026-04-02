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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageDownloadResult;
import tech.lamprism.lampray.storage.StorageProvider;
import tech.lamprism.lampray.storage.StorageReference;
import tech.lamprism.lampray.storage.StorageReferenceRequest;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.StorageUploadSession;
import tech.lamprism.lampray.storage.StorageUploadSessionDetails;
import tech.lamprism.lampray.storage.access.StorageAccessService;
import tech.lamprism.lampray.storage.domain.StorageUploadSessionModel;
import tech.lamprism.lampray.storage.file.StorageFileDeletionService;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.session.StorageUploadSessionManager;
import tech.lamprism.lampray.storage.session.UploadSessionStatus;
import tech.lamprism.lampray.storage.upload.workflow.DirectUploadCompletionWorkflow;
import tech.lamprism.lampray.storage.upload.workflow.DirectUploadCompletionWorkflowContext;
import tech.lamprism.lampray.storage.upload.workflow.ProxyUploadWorkflow;
import tech.lamprism.lampray.storage.upload.workflow.ProxyUploadWorkflowContext;
import tech.lamprism.lampray.storage.upload.workflow.TrustedUploadWorkflow;
import tech.lamprism.lampray.storage.upload.workflow.TrustedUploadWorkflowContext;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.DataErrorCode;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author RollW
 */
@Service
@Transactional(readOnly = true)
public class StorageProviderImpl implements StorageProvider {
    private final StorageAccessService storageAccessService;
    private final StorageFileRepository storageFileRepository;
    private final StorageFileDeletionService storageFileDeletionService;
    private final StorageUploadSessionManager storageUploadSessionManager;
    private final ProxyUploadWorkflow proxyUploadWorkflow;
    private final DirectUploadCompletionWorkflow directUploadCompletionWorkflow;
    private final TrustedUploadWorkflow trustedUploadWorkflow;

    public StorageProviderImpl(StorageAccessService storageAccessService,
                               StorageFileRepository storageFileRepository,
                               StorageFileDeletionService storageFileDeletionService,
                               StorageUploadSessionManager storageUploadSessionManager,
                               ProxyUploadWorkflow proxyUploadWorkflow,
                               DirectUploadCompletionWorkflow directUploadCompletionWorkflow,
                               TrustedUploadWorkflow trustedUploadWorkflow) {
        this.storageAccessService = storageAccessService;
        this.storageFileRepository = storageFileRepository;
        this.storageFileDeletionService = storageFileDeletionService;
        this.storageUploadSessionManager = storageUploadSessionManager;
        this.proxyUploadWorkflow = proxyUploadWorkflow;
        this.directUploadCompletionWorkflow = directUploadCompletionWorkflow;
        this.trustedUploadWorkflow = trustedUploadWorkflow;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FileStorage saveFile(InputStream inputStream) throws IOException {
        return trustedUploadWorkflow.execute(new TrustedUploadWorkflowContext(inputStream));
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public StorageUploadSession createUploadSession(StorageUploadRequest request,
                                                    Long userId) throws IOException {
        return storageUploadSessionManager.createUploadSession(request, userId);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FileStorage uploadFileContent(String uploadId,
                                         InputStream inputStream,
                                         Long userId) throws IOException {
        StorageUploadSessionModel uploadSession = storageUploadSessionManager.requireActiveUploadSession(uploadId, userId);
        if (uploadSession.getUploadMode() != StorageUploadMode.PROXY) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session requires completion after direct upload: " + uploadSession.getUploadId());
        }
        return proxyUploadWorkflow.execute(new ProxyUploadWorkflowContext(uploadSession, inputStream));
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FileStorage completeUpload(String uploadId,
                                      Long userId) throws IOException {
        StorageUploadSessionModel uploadSession = storageUploadSessionManager.requireUploadSessionAuthorized(uploadId, userId);
        if (uploadSession.getStatus() == UploadSessionStatus.COMPLETED) {
            return toFileStorage(requireStoredFile(uploadSession.getFileId()));
        }
        uploadSession = storageUploadSessionManager.requireActiveUploadSession(uploadId, userId);
        if (uploadSession.getUploadMode() != StorageUploadMode.DIRECT) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session requires proxy upload: " + uploadSession.getUploadId());
        }
        return directUploadCompletionWorkflow.execute(new DirectUploadCompletionWorkflowContext(uploadSession));
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public StorageUploadSessionDetails getUploadSession(String uploadId,
                                                        Long userId) {
        return storageUploadSessionManager.getUploadSession(uploadId, userId);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void deleteFile(String fileId,
                           Long userId) throws IOException {
        storageFileDeletionService.deleteFile(fileId, userId);
    }

    @Override
    public StorageDownloadResult resolveDownload(String fileId,
                                                 Long userId) throws IOException {
        return storageAccessService.resolveDownload(fileId, userId);
    }

    @Override
    public StorageReference resolveStorageReference(String id,
                                                    StorageReferenceRequest request,
                                                    Long userId) throws IOException {
        return storageAccessService.resolveStorageReference(id, request, userId);
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
