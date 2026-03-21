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
import tech.lamprism.lampray.storage.access.StorageAccessService;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageDownloadResult;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageProvider;
import tech.lamprism.lampray.storage.StorageReference;
import tech.lamprism.lampray.storage.StorageReferenceRequest;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.StorageUploadSession;
import tech.lamprism.lampray.storage.StorageUploadSessionDetails;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.materialization.BlobMaterializationRequest;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.materialization.StorageBlobMaterializationService;
import tech.lamprism.lampray.storage.materialization.TempUpload;
import tech.lamprism.lampray.storage.materialization.TempUploadWriter;
import tech.lamprism.lampray.storage.monitoring.StorageTrafficRecorder;
import tech.lamprism.lampray.storage.policy.StorageContentPolicy;
import tech.lamprism.lampray.storage.policy.UploadRequestValidator;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.UploadSessionStatus;
import tech.lamprism.lampray.storage.routing.StorageGroupRouter;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.session.StorageUploadSessionService;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.DataErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author RollW
 */
@Service
@Transactional(readOnly = true)
public class StorageProviderImpl implements StorageProvider {
    private final StorageFileRepository storageFileRepository;
    private final StorageGroupRouter storageGroupRouter;
    private final StorageAccessService storageAccessService;
    private final StorageRuntimeConfig runtimeSettings;
    private final StorageUploadSessionService storageUploadSessionService;
    private final StorageContentPolicy storageContentPolicy;
    private final UploadRequestValidator uploadRequestValidator;
    private final TempUploadWriter tempUploadWriter;
    private final StorageTrafficRecorder storageTrafficRecorder;
    private final StorageBlobMaterializationService storageBlobMaterializationService;
    private final StorageFilePersistenceService storageFilePersistenceService;
    private final Map<StorageUploadMode, StorageUploadStrategy> uploadStrategies;

    public StorageProviderImpl(StorageFileRepository storageFileRepository,
                                 StorageGroupRouter storageGroupRouter,
                                 StorageAccessService storageAccessService,
                                 StorageRuntimeConfig runtimeSettings,
                                 StorageUploadSessionService storageUploadSessionService,
                                 StorageContentPolicy storageContentPolicy,
                                  UploadRequestValidator uploadRequestValidator,
                                  TempUploadWriter tempUploadWriter,
                                  StorageTrafficRecorder storageTrafficRecorder,
                                  StorageBlobMaterializationService storageBlobMaterializationService,
                                  StorageFilePersistenceService storageFilePersistenceService,
                                  List<StorageUploadStrategy> uploadStrategies) {
        this.storageFileRepository = storageFileRepository;
        this.storageGroupRouter = storageGroupRouter;
        this.storageAccessService = storageAccessService;
        this.runtimeSettings = runtimeSettings;
        this.storageUploadSessionService = storageUploadSessionService;
        this.storageContentPolicy = storageContentPolicy;
        this.uploadRequestValidator = uploadRequestValidator;
        this.tempUploadWriter = tempUploadWriter;
        this.storageTrafficRecorder = storageTrafficRecorder;
        this.storageBlobMaterializationService = storageBlobMaterializationService;
        this.storageFilePersistenceService = storageFilePersistenceService;
        this.uploadStrategies = indexUploadStrategies(uploadStrategies);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FileStorage saveFile(InputStream inputStream) throws IOException {
        String groupName = runtimeSettings.getDefaultGroup();
        StorageWritePlan writePlan = selectWritePlan(groupName);
        StorageGroupConfig groupSettings = writePlan.getGroupSettings();
        String fileName = "upload.bin";
        String mimeType = storageContentPolicy.requireMimeType("application/octet-stream");
        FileType fileType = storageContentPolicy.resolveFileType(mimeType);
        TempUpload tempUpload = tempUploadWriter.write(inputStream, groupSettings.getMaxSizeBytes());
        try {
            storageTrafficRecorder.recordProxyUpload(groupName, writePlan.getPrimaryBackend(), tempUpload.getSize());
            uploadRequestValidator.validate(
                    new StorageUploadRequest(groupName, fileName, tempUpload.getSize(), mimeType, tempUpload.getChecksumSha256()),
                    groupSettings,
                    fileType
            );
            PreparedBlobMaterialization preparedBlob = storageBlobMaterializationService.prepareBlobMaterialization(
                    BlobMaterializationRequest.forTempUpload(
                            writePlan,
                            mimeType,
                            fileType,
                            tempUpload.getSize(),
                            tempUpload.getChecksumSha256(),
                            tempUpload.getPath()
                    )
            );
            return storageFilePersistenceService.persistTrustedUpload(groupName, fileName, mimeType, fileType, null, preparedBlob);
        } finally {
            Files.deleteIfExists(tempUpload.getPath());
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public StorageUploadSession createUploadSession(StorageUploadRequest request,
                                                    Long userId) throws IOException {
        return storageUploadSessionService.createUploadSession(request, userId);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FileStorage uploadFileContent(String uploadId,
                                         InputStream inputStream,
                                         Long userId) throws IOException {
        StorageUploadSessionEntity uploadSession = storageUploadSessionService.requireActiveUploadSession(uploadId, userId);
        if (uploadSession.getUploadMode() != StorageUploadMode.PROXY) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session requires completion after direct upload: " + uploadId);
        }
        return uploadStrategy(uploadSession.getUploadMode()).uploadContent(uploadSession, inputStream);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FileStorage completeUpload(String uploadId,
                                      Long userId) throws IOException {
        StorageUploadSessionEntity uploadSession = storageUploadSessionService.requireUploadSession(uploadId);
        if (uploadSession.getStatus() == UploadSessionStatus.COMPLETED) {
            return requireFileEntity(uploadSession.getFileId()).lock();
        }
        uploadSession = storageUploadSessionService.requireActiveUploadSession(uploadId, userId);
        if (uploadSession.getUploadMode() != StorageUploadMode.DIRECT) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session requires proxy upload: " + uploadId);
        }

        return uploadStrategy(uploadSession.getUploadMode()).completeUpload(uploadSession);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public StorageUploadSessionDetails getUploadSession(String uploadId,
                                                        Long userId) {
        return storageUploadSessionService.getUploadSession(uploadId, userId);
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

    private StorageFileEntity requireFileEntity(String fileId) {
        return storageFileRepository.findById(fileId)
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "File not found: " + fileId));
    }

    private StorageWritePlan selectWritePlan(String groupName) {
        try {
            return storageGroupRouter.selectWritePlan(groupName);
        } catch (IllegalStateException exception) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, exception.getMessage());
        }
    }

    private StorageUploadStrategy uploadStrategy(StorageUploadMode mode) {
        StorageUploadStrategy strategy = uploadStrategies.get(mode);
        if (strategy == null) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Unsupported upload mode: " + mode);
        }
        return strategy;
    }

    private Map<StorageUploadMode, StorageUploadStrategy> indexUploadStrategies(List<StorageUploadStrategy> strategies) {
        Map<StorageUploadMode, StorageUploadStrategy> indexed = new LinkedHashMap<>();
        for (StorageUploadStrategy strategy : strategies) {
            StorageUploadStrategy previous = indexed.put(strategy.mode(), strategy);
            if (previous != null) {
                throw new IllegalStateException("Duplicate storage upload strategy for mode: " + strategy.mode());
            }
        }
        return Map.copyOf(indexed);
    }
}
