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
import tech.lamprism.lampray.storage.backend.BlobStoreLocator;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementRepository;
import tech.lamprism.lampray.storage.persistence.StorageBlobRepository;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.routing.StorageGroupRouter;

import java.io.IOException;

/**
 * @author RollW
 */
@Service
public class StorageBlobCleaner {
    private final BlobStoreLocator blobStoreLocator;
    private final StorageBlobRepository storageBlobRepository;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final StorageUploadSessionRepository storageUploadSessionRepository;

    public StorageBlobCleaner(BlobStoreLocator blobStoreLocator,
                              StorageBlobRepository storageBlobRepository,
                              StorageBlobPlacementRepository storageBlobPlacementRepository,
                              StorageUploadSessionRepository storageUploadSessionRepository) {
        this.blobStoreLocator = blobStoreLocator;
        this.storageBlobRepository = storageBlobRepository;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.storageUploadSessionRepository = storageUploadSessionRepository;
    }

    public void cleanupOrphanedObjects(StorageGroupRouter.StorageBlobCleanupPlan cleanupPlan) throws IOException {
        for (StorageGroupRouter.StorageBlobCleanupTarget target : cleanupPlan.targets()) {
            if (isStillReferenced(target.backendName(), target.objectKey())) {
                continue;
            }
            blobStoreLocator.require(target.backendName()).delete(target.objectKey());
        }
    }

    public boolean cleanupRetainedBlobObjects(String blobId,
                                              StorageGroupRouter.StorageBlobCleanupPlan cleanupPlan) throws IOException {
        for (StorageGroupRouter.StorageBlobCleanupTarget target : cleanupPlan.targets()) {
            if (isStillReferencedForPurge(blobId, target.backendName(), target.objectKey())) {
                continue;
            }
            var blobStore = blobStoreLocator.require(target.backendName());
            if (!blobStore.exists(target.objectKey())) {
                continue;
            }
            if (!blobStore.delete(target.objectKey()) && blobStore.exists(target.objectKey())) {
                return false;
            }
        }
        return true;
    }

    private boolean isStillReferenced(String backendName,
                                      String objectKey) {
        return storageBlobRepository.existsByPrimaryBackendAndPrimaryObjectKey(backendName, objectKey)
                || storageBlobPlacementRepository.existsByBackendNameAndObjectKey(backendName, objectKey)
                || storageUploadSessionRepository.existsPendingSessionByPrimaryBackendAndObjectKey(backendName, objectKey);
    }

    private boolean isStillReferencedForPurge(String blobId,
                                              String backendName,
                                              String objectKey) {
        return storageBlobRepository.existsOtherByPrimaryBackendAndPrimaryObjectKey(backendName, objectKey, blobId)
                || storageBlobPlacementRepository.existsByBackendNameAndObjectKey(backendName, objectKey)
                || storageUploadSessionRepository.existsPendingSessionByPrimaryBackendAndObjectKey(backendName, objectKey);
    }
}
