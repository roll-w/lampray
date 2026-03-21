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
import org.springframework.util.StringUtils;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.backend.BlobStoreRegistry;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.persistence.UploadSessionStatus;
import tech.lamprism.lampray.storage.store.BlobStore;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class DefaultStorageUploadSessionCleanupService implements StorageUploadSessionCleanupService {
    private final StorageRuntimeConfig runtimeSettings;
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final StorageFileRepository storageFileRepository;
    private final BlobStoreRegistry blobStoreRegistry;

    public DefaultStorageUploadSessionCleanupService(StorageRuntimeConfig runtimeSettings,
                                                    StorageUploadSessionRepository storageUploadSessionRepository,
                                                    StorageFileRepository storageFileRepository,
                                                    BlobStoreRegistry blobStoreRegistry) {
        this.runtimeSettings = runtimeSettings;
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.storageFileRepository = storageFileRepository;
        this.blobStoreRegistry = blobStoreRegistry;
    }

    @Override
    public int expireOverdueSessions(OffsetDateTime now) {
        List<StorageUploadSessionEntity> overdueSessions = storageUploadSessionRepository.findAllByStatusAndExpiresAtBefore(
                UploadSessionStatus.PENDING,
                now
        );
        for (StorageUploadSessionEntity overdueSession : overdueSessions) {
            overdueSession.setStatus(UploadSessionStatus.EXPIRED);
            overdueSession.setUpdateTime(now);
        }
        if (!overdueSessions.isEmpty()) {
            storageUploadSessionRepository.saveAll(overdueSessions);
        }
        return overdueSessions.size();
    }

    @Override
    public int purgeExpiredSessions(OffsetDateTime now) throws IOException {
        OffsetDateTime expiredRetentionCutoff = now.minusSeconds(runtimeSettings.getCleanupExpiredUploadRetainSeconds());
        OffsetDateTime orphanCleanupCutoff = now.minusSeconds(runtimeSettings.getCleanupOrphanUploadExpireSeconds());
        List<StorageUploadSessionEntity> expiredSessions = storageUploadSessionRepository.findAllByStatus(
                UploadSessionStatus.EXPIRED
        );
        List<StorageUploadSessionEntity> deletableSessions = new java.util.ArrayList<>();
        for (StorageUploadSessionEntity expiredSession : expiredSessions) {
            if (!isReadyToPurgeExpiredSession(expiredSession, expiredRetentionCutoff, orphanCleanupCutoff)) {
                continue;
            }
            if (cleanupExpiredUploadObject(expiredSession)) {
                deletableSessions.add(expiredSession);
            }
        }
        if (!deletableSessions.isEmpty()) {
            storageUploadSessionRepository.deleteAll(deletableSessions);
        }
        return deletableSessions.size();
    }

    @Override
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

    private boolean isReadyToPurgeExpiredSession(StorageUploadSessionEntity uploadSession,
                                                 OffsetDateTime expiredRetentionCutoff,
                                                 OffsetDateTime orphanCleanupCutoff) {
        if (uploadSession.getUpdateTime().isAfter(expiredRetentionCutoff)) {
            return false;
        }
        if (!requiresOrphanCleanup(uploadSession)) {
            return true;
        }
        return !uploadSession.getExpiresAt().isAfter(orphanCleanupCutoff);
    }

    private boolean cleanupExpiredUploadObject(StorageUploadSessionEntity uploadSession) throws IOException {
        if (!requiresOrphanCleanup(uploadSession)) {
            return true;
        }
        BlobStore blobStore = blobStoreRegistry.find(uploadSession.getPrimaryBackend()).orElse(null);
        if (blobStore == null) {
            return false;
        }
        String objectKey = uploadSession.getObjectKey();
        if (!blobStore.exists(objectKey)) {
            return true;
        }
        return blobStore.delete(objectKey);
    }

    private boolean requiresOrphanCleanup(StorageUploadSessionEntity uploadSession) {
        if (uploadSession.getUploadMode() != StorageUploadMode.DIRECT) {
            return false;
        }
        if (!StringUtils.hasText(uploadSession.getPrimaryBackend()) || !StringUtils.hasText(uploadSession.getObjectKey())) {
            return false;
        }
        return !storageFileRepository.existsById(uploadSession.getFileId());
    }
}
