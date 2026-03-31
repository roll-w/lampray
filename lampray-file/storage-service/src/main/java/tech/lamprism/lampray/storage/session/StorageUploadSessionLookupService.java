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
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.persistence.UploadSessionStatus;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.DataErrorCode;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
@Service
public class StorageUploadSessionLookupService {
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final UploadSessionAuthorizationValidator uploadSessionAuthorizationValidator;
    private final StorageUploadSessionLifecycleService storageUploadSessionLifecycleService;

    public StorageUploadSessionLookupService(StorageUploadSessionRepository storageUploadSessionRepository,
                                             UploadSessionAuthorizationValidator uploadSessionAuthorizationValidator,
                                             StorageUploadSessionLifecycleService storageUploadSessionLifecycleService) {
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.uploadSessionAuthorizationValidator = uploadSessionAuthorizationValidator;
        this.storageUploadSessionLifecycleService = storageUploadSessionLifecycleService;
    }

    public StorageUploadSessionEntity requireUploadSession(String uploadId) {
        return storageUploadSessionRepository.findById(uploadId)
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "Upload session not found: " + uploadId));
    }

    public StorageUploadSessionEntity requireUploadSessionAuthorized(String uploadId,
                                                                     Long userId) {
        StorageUploadSessionEntity uploadSession = requireUploadSession(uploadId);
        uploadSessionAuthorizationValidator.ensureAuthorized(uploadSession, userId);
        return uploadSession;
    }

    public StorageUploadSessionEntity requireActiveUploadSession(String uploadId,
                                                                 Long userId) {
        StorageUploadSessionEntity uploadSession = requireUploadSessionAuthorized(uploadId, userId);
        if (uploadSession.getStatus() != UploadSessionStatus.PENDING) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session is not pending: " + uploadSession.getUploadId());
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (uploadSession.getExpiresAt().isBefore(now)) {
            storageUploadSessionLifecycleService.expire(uploadSession, now);
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session has expired: " + uploadSession.getUploadId());
        }
        return uploadSession;
    }
}
