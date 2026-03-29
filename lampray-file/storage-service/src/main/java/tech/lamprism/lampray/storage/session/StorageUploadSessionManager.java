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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.StorageUploadSession;
import tech.lamprism.lampray.storage.StorageUploadSessionDetails;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * @author RollW
 */
@Service
@Transactional(readOnly = true)
public class StorageUploadSessionManager implements StorageUploadSessionService {
    private final StorageUploadSessionCreationService storageUploadSessionCreationService;
    private final StorageUploadSessionLookupService storageUploadSessionLookupService;
    private final StorageUploadSessionDetailsService storageUploadSessionDetailsService;
    private final StorageUploadSessionLifecycleService storageUploadSessionLifecycleService;

    public StorageUploadSessionManager(StorageUploadSessionCreationService storageUploadSessionCreationService,
                                       StorageUploadSessionLookupService storageUploadSessionLookupService,
                                       StorageUploadSessionDetailsService storageUploadSessionDetailsService,
                                       StorageUploadSessionLifecycleService storageUploadSessionLifecycleService) {
        this.storageUploadSessionCreationService = storageUploadSessionCreationService;
        this.storageUploadSessionLookupService = storageUploadSessionLookupService;
        this.storageUploadSessionDetailsService = storageUploadSessionDetailsService;
        this.storageUploadSessionLifecycleService = storageUploadSessionLifecycleService;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public StorageUploadSession createUploadSession(StorageUploadRequest request,
                                                    Long userId) throws IOException {
        return storageUploadSessionCreationService.createUploadSession(request, userId);
    }

    @Override
    public StorageUploadSessionEntity requireUploadSession(String uploadId) {
        return storageUploadSessionLookupService.requireUploadSession(uploadId);
    }

    @Override
    public StorageUploadSessionEntity requireUploadSessionAuthorized(String uploadId,
                                                                     Long userId) {
        return storageUploadSessionLookupService.requireUploadSessionAuthorized(uploadId, userId);
    }

    @Override
    public StorageUploadSessionEntity requireActiveUploadSession(String uploadId,
                                                                 Long userId) {
        return storageUploadSessionLookupService.requireActiveUploadSession(uploadId, userId);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public StorageUploadSessionDetails getUploadSession(String uploadId,
                                                        Long userId) {
        return storageUploadSessionDetailsService.getUploadSession(uploadId, userId);
    }

    @Override
    public void expirePendingUploadSession(String uploadId) {
        storageUploadSessionLifecycleService.expirePendingUploadSession(uploadId);
    }

    @Override
    public void markCompleted(StorageUploadSessionEntity uploadSession,
                              OffsetDateTime now) {
        storageUploadSessionLifecycleService.markCompleted(uploadSession, now);
    }
}
