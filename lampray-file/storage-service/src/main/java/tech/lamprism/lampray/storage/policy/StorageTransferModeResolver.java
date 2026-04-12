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

package tech.lamprism.lampray.storage.policy;

import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.StorageDownloadMode;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageGroupDownloadPolicy;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.store.BlobStoreCapability;

/**
 * @author RollW
 */
public final class StorageTransferModeResolver {
    private final StorageRuntimeConfig runtimeSettings;

    public StorageTransferModeResolver(StorageRuntimeConfig runtimeSettings) {
        this.runtimeSettings = runtimeSettings;
    }

    public StorageUploadMode resolveUploadMode(StorageUploadRequest request,
                                               String checksum,
                                               BlobStore blobStore) {
        if (!runtimeSettings.getDirectAccessEnabled() || !blobStore.supports(BlobStoreCapability.DIRECT_UPLOAD)) {
            return StorageUploadMode.PROXY;
        }
        if (request.getSize() == null || request.getSize() <= runtimeSettings.getUploadProxyThresholdBytes()) {
            return StorageUploadMode.PROXY;
        }
        if (checksum == null) {
            return StorageUploadMode.PROXY;
        }
        return StorageUploadMode.DIRECT;
    }

    public StorageDownloadMode resolveDownloadMode(FileStorage fileStorage,
                                                   StorageGroupConfig groupSettings,
                                                   BlobStore blobStore) {
        if (!runtimeSettings.getDirectAccessEnabled() || !blobStore.supports(BlobStoreCapability.DIRECT_DOWNLOAD)) {
            return StorageDownloadMode.PROXY;
        }
        if (groupSettings.getDownloadPolicy() == StorageGroupDownloadPolicy.PROXY) {
            return StorageDownloadMode.PROXY;
        }
        if (groupSettings.getDownloadPolicy() == StorageGroupDownloadPolicy.DIRECT) {
            return StorageDownloadMode.DIRECT;
        }
        if (fileStorage.getFileSize() > runtimeSettings.getDownloadProxyThresholdBytes()) {
            return StorageDownloadMode.DIRECT;
        }
        return StorageDownloadMode.PROXY;
    }
}
