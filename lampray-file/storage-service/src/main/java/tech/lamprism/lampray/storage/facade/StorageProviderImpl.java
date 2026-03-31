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
import tech.lamprism.lampray.storage.StorageDownloadResult;
import tech.lamprism.lampray.storage.StorageProvider;
import tech.lamprism.lampray.storage.StorageReference;
import tech.lamprism.lampray.storage.StorageReferenceRequest;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.StorageUploadSession;
import tech.lamprism.lampray.storage.StorageUploadSessionDetails;
import tech.lamprism.lampray.storage.access.StorageAccessService;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author RollW
 */
@Service
@Transactional(readOnly = true)
public class StorageProviderImpl implements StorageProvider {
    private final StorageAccessService storageAccessService;
    private final StorageUploadManager storageUploadManager;

    public StorageProviderImpl(StorageAccessService storageAccessService,
                               StorageUploadManager storageUploadManager) {
        this.storageAccessService = storageAccessService;
        this.storageUploadManager = storageUploadManager;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FileStorage saveFile(InputStream inputStream) throws IOException {
        return storageUploadManager.saveFile(inputStream);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public StorageUploadSession createUploadSession(StorageUploadRequest request,
                                                    Long userId) throws IOException {
        return storageUploadManager.createUploadSession(request, userId);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FileStorage uploadFileContent(String uploadId,
                                         InputStream inputStream,
                                         Long userId) throws IOException {
        return storageUploadManager.uploadFileContent(uploadId, inputStream, userId);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FileStorage completeUpload(String uploadId,
                                      Long userId) throws IOException {
        return storageUploadManager.completeUpload(uploadId, userId);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public StorageUploadSessionDetails getUploadSession(String uploadId,
                                                        Long userId) {
        return storageUploadManager.getUploadSession(uploadId, userId);
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

}
