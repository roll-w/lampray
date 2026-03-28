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
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.StorageUploadSession;
import tech.lamprism.lampray.storage.StorageUploadSessionDetails;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.UploadSessionStatus;
import tech.lamprism.lampray.storage.persistence.file.StorageFileLookupService;
import tech.lamprism.lampray.storage.session.StorageUploadSessionService;
import tech.lamprism.lampray.storage.upload.workflow.TrustedUploadWorkflowContext;
import tech.lamprism.lampray.storage.upload.workflow.TrustedUploadWorkflowFactory;

import java.io.IOException;
import java.io.InputStream;

@Service
public class StorageUploadManager {
    private final StorageFileLookupService storageFileLookupService;
    private final StorageUploadSessionService storageUploadSessionService;
    private final TrustedUploadWorkflowFactory trustedUploadWorkflowFactory;
    private final StorageUploadOperationRouter storageUploadOperationRouter;

    public StorageUploadManager(StorageFileLookupService storageFileLookupService,
                                StorageUploadSessionService storageUploadSessionService,
                                TrustedUploadWorkflowFactory trustedUploadWorkflowFactory,
                                StorageUploadOperationRouter storageUploadOperationRouter) {
        this.storageFileLookupService = storageFileLookupService;
        this.storageUploadSessionService = storageUploadSessionService;
        this.trustedUploadWorkflowFactory = trustedUploadWorkflowFactory;
        this.storageUploadOperationRouter = storageUploadOperationRouter;
    }

    public FileStorage saveFile(InputStream inputStream) throws IOException {
        return trustedUploadWorkflowFactory.create().execute(new TrustedUploadWorkflowContext(inputStream));
    }

    public StorageUploadSession createUploadSession(StorageUploadRequest request,
                                                    Long userId) throws IOException {
        return storageUploadSessionService.createUploadSession(request, userId);
    }

    public FileStorage uploadFileContent(String uploadId,
                                         InputStream inputStream,
                                         Long userId) throws IOException {
        StorageUploadSessionEntity uploadSession = storageUploadSessionService.requireActiveUploadSession(uploadId, userId);
        return storageUploadOperationRouter.uploadContent(uploadSession, inputStream);
    }

    public FileStorage completeUpload(String uploadId,
                                      Long userId) throws IOException {
        StorageUploadSessionEntity uploadSession = storageUploadSessionService.requireUploadSessionAuthorized(uploadId, userId);
        if (uploadSession.getStatus() == UploadSessionStatus.COMPLETED) {
            return storageFileLookupService.requireFileEntity(uploadSession.getFileId()).lock();
        }
        uploadSession = storageUploadSessionService.requireActiveUploadSession(uploadId, userId);
        return storageUploadOperationRouter.completeUpload(uploadSession);
    }

    public StorageUploadSessionDetails getUploadSession(String uploadId,
                                                        Long userId) {
        return storageUploadSessionService.getUploadSession(uploadId, userId);
    }
}
