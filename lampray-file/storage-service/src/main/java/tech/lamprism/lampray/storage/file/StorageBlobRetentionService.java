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

package tech.lamprism.lampray.storage.file;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.file.workflow.PurgeRetainedBlobWorkflow;
import tech.lamprism.lampray.storage.file.workflow.PurgeRetainedBlobWorkflowContext;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobRepository;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * @author RollW
 */
@Service
public class StorageBlobRetentionService {
    private final StorageBlobRepository storageBlobRepository;
    private final StorageRuntimeConfig storageRuntimeConfig;
    private final PurgeRetainedBlobWorkflow purgeRetainedBlobWorkflow;

    public StorageBlobRetentionService(StorageBlobRepository storageBlobRepository,
                                       StorageRuntimeConfig storageRuntimeConfig,
                                       PurgeRetainedBlobWorkflow purgeRetainedBlobWorkflow) {
        this.storageBlobRepository = storageBlobRepository;
        this.storageRuntimeConfig = storageRuntimeConfig;
        this.purgeRetainedBlobWorkflow = purgeRetainedBlobWorkflow;
    }

    public int purgeRetainedBlobs(OffsetDateTime now) throws IOException {
        OffsetDateTime retentionCutoff = resolveRetentionCutoff(now);
        int purged = 0;
        for (StorageBlobEntity candidate : storageBlobRepository.findAllByOrphanedAtBefore(retentionCutoff)) {
            if (purgeRetainedBlobWorkflow.execute(
                    new PurgeRetainedBlobWorkflowContext(
                            candidate.getBlobId(),
                            candidate.getContentChecksum(),
                            retentionCutoff
                    )
            )) {
                purged++;
            }
        }
        return purged;
    }

    private OffsetDateTime resolveRetentionCutoff(OffsetDateTime now) {
        long retentionSeconds = storageRuntimeConfig.getCleanupDeletedBlobRetainSeconds();
        if (retentionSeconds <= 0) {
            return now;
        }
        return now.minusSeconds(retentionSeconds);
    }
}
