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

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.materialization.BlobMaterializationRequest;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.materialization.StorageBlobMaterializationService;
import tech.lamprism.lampray.storage.materialization.TempUpload;
import tech.lamprism.lampray.storage.materialization.TempUploadWriter;
import tech.lamprism.lampray.storage.monitoring.StorageTrafficRecorder;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.policy.UploadedContentValidator;
import tech.lamprism.lampray.storage.routing.StorageGroupRouter;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Component
public class ProxyStorageUploadStrategy implements StorageUploadStrategy {
    private final TempUploadWriter tempUploadWriter;
    private final UploadedContentValidator uploadedContentValidator;
    private final StorageBlobMaterializationService storageBlobMaterializationService;
    private final StorageFilePersistenceService storageFilePersistenceService;
    private final StorageGroupRouter storageGroupRouter;
    private final StorageTrafficRecorder storageTrafficRecorder;

    public ProxyStorageUploadStrategy(TempUploadWriter tempUploadWriter,
                                      UploadedContentValidator uploadedContentValidator,
                                      StorageBlobMaterializationService storageBlobMaterializationService,
                                      StorageFilePersistenceService storageFilePersistenceService,
                                      StorageGroupRouter storageGroupRouter,
                                      StorageTrafficRecorder storageTrafficRecorder) {
        this.tempUploadWriter = tempUploadWriter;
        this.uploadedContentValidator = uploadedContentValidator;
        this.storageBlobMaterializationService = storageBlobMaterializationService;
        this.storageFilePersistenceService = storageFilePersistenceService;
        this.storageGroupRouter = storageGroupRouter;
        this.storageTrafficRecorder = storageTrafficRecorder;
    }

    @Override
    public StorageUploadMode mode() {
        return StorageUploadMode.PROXY;
    }

    @Override
    public FileStorage uploadContent(StorageUploadSessionEntity uploadSession,
                                     InputStream inputStream) throws IOException {
        StorageWritePlan writePlan = restoreWritePlan(uploadSession);
        StorageGroupConfig groupSettings = writePlan.getGroupSettings();
        TempUpload tempUpload = tempUploadWriter.write(inputStream, groupSettings.getMaxSizeBytes());
        try {
            storageTrafficRecorder.recordProxyUpload(uploadSession.getGroupName(), uploadSession.getPrimaryBackend(), tempUpload.getSize());
            uploadedContentValidator.validate(uploadSession, tempUpload, groupSettings);
            PreparedBlobMaterialization preparedBlob = storageBlobMaterializationService.prepareBlobMaterialization(
                    BlobMaterializationRequest.forTempUpload(
                            writePlan,
                            uploadSession.getMimeType(),
                            uploadSession.getFileType(),
                            tempUpload.getSize(),
                            tempUpload.getChecksumSha256(),
                            tempUpload.getPath()
                    )
            );
            return storageFilePersistenceService.persistSessionUpload(uploadSession, preparedBlob);
        } finally {
            Files.deleteIfExists(tempUpload.getPath());
        }
    }

    private StorageWritePlan restoreWritePlan(StorageUploadSessionEntity uploadSession) {
        try {
            return storageGroupRouter.restoreWritePlan(uploadSession.getGroupName(), uploadSession.getPrimaryBackend());
        } catch (IllegalStateException exception) {
            throw new tech.lamprism.lampray.storage.StorageException(
                    tech.rollw.common.web.CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    exception.getMessage()
            );
        }
    }
}
