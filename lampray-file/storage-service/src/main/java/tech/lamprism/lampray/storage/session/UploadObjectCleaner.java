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
import tech.lamprism.lampray.storage.backend.BlobStoreRegistry;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.store.BlobStore;

import java.io.IOException;

/**
 * @author RollW
 */
@Service
public class UploadObjectCleaner {
    private final BlobStoreRegistry blobStoreRegistry;

    public UploadObjectCleaner(BlobStoreRegistry blobStoreRegistry) {
        this.blobStoreRegistry = blobStoreRegistry;
    }

    public boolean cleanup(StorageUploadSessionEntity uploadSession) throws IOException {
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
}
