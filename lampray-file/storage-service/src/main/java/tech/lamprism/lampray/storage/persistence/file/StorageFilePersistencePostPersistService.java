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

package tech.lamprism.lampray.storage.persistence.file;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.materialization.hook.StorageMaterializationContext;
import tech.lamprism.lampray.storage.materialization.hook.StorageMaterializationHookNotifier;

/**
 * @author RollW
 */
@Service
public class StorageFilePersistencePostPersistService {
    private final StorageMaterializationHookNotifier storageMaterializationHookNotifier;

    public StorageFilePersistencePostPersistService(StorageMaterializationHookNotifier storageMaterializationHookNotifier) {
        this.storageMaterializationHookNotifier = storageMaterializationHookNotifier;
    }

    public void afterSessionUploadPersisted(PersistedMaterialization persistedMaterialization,
                                            String groupName,
                                            Long ownerUserId) {
        notifyHooks(persistedMaterialization.getFileStorage(), persistedMaterialization.getBlobId(), groupName, ownerUserId);
    }

    public void afterTrustedUploadPersisted(PersistedMaterialization persistedMaterialization,
                                            String groupName,
                                            Long ownerUserId) {
        notifyHooks(persistedMaterialization.getFileStorage(), persistedMaterialization.getBlobId(), groupName, ownerUserId);
    }

    private void notifyHooks(FileStorage fileStorage,
                             String blobId,
                             String groupName,
                             Long ownerUserId) {
        StorageMaterializationContext context = new StorageMaterializationContext(fileStorage, blobId, groupName, ownerUserId);
        storageMaterializationHookNotifier.notifyAfterMaterialized(context);
    }
}
