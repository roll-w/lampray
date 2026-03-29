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
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageUploadSessionDetails;
import tech.lamprism.lampray.storage.StorageUploadSessionState;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.rollw.common.web.DataErrorCode;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
@Service
public class StorageUploadSessionDetailsService {
    private final StorageUploadSessionLookupService storageUploadSessionLookupService;
    private final UploadSessionAuthorizationValidator uploadSessionAuthorizationValidator;
    private final StorageFileRepository storageFileRepository;

    public StorageUploadSessionDetailsService(StorageUploadSessionLookupService storageUploadSessionLookupService,
                                              UploadSessionAuthorizationValidator uploadSessionAuthorizationValidator,
                                              StorageFileRepository storageFileRepository) {
        this.storageUploadSessionLookupService = storageUploadSessionLookupService;
        this.uploadSessionAuthorizationValidator = uploadSessionAuthorizationValidator;
        this.storageFileRepository = storageFileRepository;
    }

    public StorageUploadSessionDetails getUploadSession(String uploadId,
                                                        Long userId) {
        StorageUploadSessionEntity uploadSession = storageUploadSessionLookupService.requireUploadSession(uploadId);
        uploadSessionAuthorizationValidator.ensureQueryable(uploadSession, userId);
        StorageUploadSessionState sessionState = StorageUploadSessionStates.resolveTrackedState(uploadSession, OffsetDateTime.now());
        FileStorage fileStorage = sessionState == StorageUploadSessionState.COMPLETED
                ? requireStoredFile(uploadSession.getFileId()).lock()
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

    private StorageFileEntity requireStoredFile(String fileId) {
        return storageFileRepository.findById(fileId)
                .orElseThrow(() -> new StorageException(
                        DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "File not found: " + fileId
                ));
    }
}
