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
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.persistence.UploadSessionStatus;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
@Service
public class StorageUploadSessionLifecycleService {
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final TransactionTemplate transactionTemplate;

    public StorageUploadSessionLifecycleService(StorageUploadSessionRepository storageUploadSessionRepository,
                                                PlatformTransactionManager transactionManager) {
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void expirePendingUploadSession(String uploadId) {
        transactionTemplate.executeWithoutResult(status -> storageUploadSessionRepository.findById(uploadId)
                .filter(uploadSession -> uploadSession.getStatus() == UploadSessionStatus.PENDING)
                .ifPresent(uploadSession -> expire(uploadSession, OffsetDateTime.now())));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expire(StorageUploadSessionEntity uploadSession,
                       OffsetDateTime now) {
        uploadSession.setStatus(UploadSessionStatus.EXPIRED);
        uploadSession.setUpdateTime(now);
        storageUploadSessionRepository.save(uploadSession);
    }

    public void markCompleted(StorageUploadSessionEntity uploadSession,
                              OffsetDateTime now) {
        uploadSession.setStatus(UploadSessionStatus.COMPLETED);
        uploadSession.setUpdateTime(now);
        storageUploadSessionRepository.save(uploadSession);
    }
}
