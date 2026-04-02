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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.file.StorageFileDeletionService;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * @author RollW
 */
@Component
public class StorageUploadSessionCleanupRunner {
    private static final Logger logger = LoggerFactory.getLogger(StorageUploadSessionCleanupRunner.class);

    private final StorageUploadSessionRetentionService retentionService;
    private final StorageFileDeletionService storageFileDeletionService;

    public StorageUploadSessionCleanupRunner(StorageUploadSessionRetentionService retentionService,
                                            StorageFileDeletionService storageFileDeletionService) {
        this.retentionService = retentionService;
        this.storageFileDeletionService = storageFileDeletionService;
    }

    @Scheduled(fixedDelayString = "#{@storageRuntimeConfig.getCleanupIntervalSeconds() * 1000}")
    public void cleanupUploadSessions() {
        OffsetDateTime now = OffsetDateTime.now();
        try {
            int expired = retentionService.expireOverdueSessions(now);
            int purgedExpired = retentionService.purgeExpiredSessions(now);
            int purgedCompleted = retentionService.purgeCompletedSessions(now);
            int purgedDeletedBlobs = storageFileDeletionService.purgeRetainedBlobs(now);
            if (expired > 0 || purgedExpired > 0 || purgedCompleted > 0 || purgedDeletedBlobs > 0) {
                logger.info(
                        "Storage upload session cleanup finished: expired={}, purgedExpired={}, purgedCompleted={}, purgedDeletedBlobs={}",
                        expired,
                        purgedExpired,
                        purgedCompleted,
                        purgedDeletedBlobs
                );
            }
        } catch (IOException | RuntimeException exception) {
            logger.warn("Storage upload session cleanup failed", exception);
        }
    }
}
