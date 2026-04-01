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
import org.springframework.transaction.annotation.Transactional;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.domain.StorageUploadSessionModel;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author RollW
 */
@Service
@Transactional
public class StorageUploadSessionRetentionService {
    private final StorageRuntimeConfig runtimeSettings;
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final StorageFileRepository storageFileRepository;
    private final UploadObjectCleaner uploadObjectCleaner;

    public StorageUploadSessionRetentionService(StorageRuntimeConfig runtimeSettings,
                                                StorageUploadSessionRepository storageUploadSessionRepository,
                                                StorageFileRepository storageFileRepository,
                                                UploadObjectCleaner uploadObjectCleaner) {
        this.runtimeSettings = runtimeSettings;
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.storageFileRepository = storageFileRepository;
        this.uploadObjectCleaner = uploadObjectCleaner;
    }

    public int expireOverdueSessions(OffsetDateTime now) {
        List<StorageUploadSessionEntity> overdueSessions = storageUploadSessionRepository.findAllByStatusAndExpiresAtBefore(
                UploadSessionStatus.PENDING,
                now
        );
        for (StorageUploadSessionEntity overdueSession : overdueSessions) {
            StorageUploadSessionModel.from(overdueSession).expire(now);
        }
        if (!overdueSessions.isEmpty()) {
            storageUploadSessionRepository.saveAll(overdueSessions);
        }
        return overdueSessions.size();
    }

    public int purgeExpiredSessions(OffsetDateTime now) throws IOException {
        OffsetDateTime expiredRetentionCutoff = now.minusSeconds(runtimeSettings.getCleanupExpiredUploadRetainSeconds());
        OffsetDateTime orphanCleanupCutoff = now.minusSeconds(runtimeSettings.getCleanupOrphanUploadExpireSeconds());
        List<StorageUploadSessionEntity> expiredSessions = storageUploadSessionRepository.findAllByStatus(
                UploadSessionStatus.EXPIRED
        );
        List<StorageUploadSessionEntity> deletableSessions = new ArrayList<>();
        for (StorageUploadSessionEntity expiredSession : expiredSessions) {
            StorageUploadSessionModel uploadSession = StorageUploadSessionModel.from(expiredSession);
            boolean hasStoredFile = storageFileRepository.existsById(uploadSession.getFileId());
            if (!uploadSession.isReadyToPurgeExpired(expiredRetentionCutoff, orphanCleanupCutoff, hasStoredFile)) {
                continue;
            }
            if (cleanupExpiredUploadObject(uploadSession, hasStoredFile)) {
                deletableSessions.add(expiredSession);
            }
        }
        if (!deletableSessions.isEmpty()) {
            storageUploadSessionRepository.deleteAll(deletableSessions);
        }
        return deletableSessions.size();
    }

    public int purgeCompletedSessions(OffsetDateTime now) {
        OffsetDateTime retentionCutoff = now.minusSeconds(runtimeSettings.getCleanupCompletedUploadRetainSeconds());
        List<StorageUploadSessionEntity> completedSessions = storageUploadSessionRepository.findAllByStatusAndUpdateTimeBefore(
                UploadSessionStatus.COMPLETED,
                retentionCutoff
        );
        if (!completedSessions.isEmpty()) {
            storageUploadSessionRepository.deleteAll(completedSessions);
        }
        return completedSessions.size();
    }

    private boolean cleanupExpiredUploadObject(StorageUploadSessionModel uploadSession,
                                               boolean hasStoredFile) throws IOException {
        if (!uploadSession.requiresOrphanCleanup(hasStoredFile)) {
            return true;
        }
        return uploadObjectCleaner.cleanup(uploadSession);
    }
}
