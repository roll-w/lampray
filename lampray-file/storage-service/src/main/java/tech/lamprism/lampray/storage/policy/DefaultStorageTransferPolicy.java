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

import org.springframework.stereotype.Component;
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
@Component
public class DefaultStorageTransferPolicy implements StorageTransferPolicy {
    private final StorageRuntimeConfig runtimeSettings;

    public DefaultStorageTransferPolicy(StorageRuntimeConfig runtimeSettings) {
        this.runtimeSettings = runtimeSettings;
    }

    @Override
    public StorageUploadMode resolveUploadMode(StorageUploadRequest request,
                                               String checksum,
                                               BlobStore blobStore) {
        if (!runtimeSettings.getDirectAccessEnabled() || !blobStore.supports(BlobStoreCapability.DIRECT_UPLOAD)) {
            return StorageUploadMode.PROXY;
        }
        if (request.getSize() == null || request.getSize() <= runtimeSettings.getUploadProxyThresholdBytes()) {
            return StorageUploadMode.PROXY;
        }
        return checksum != null ? StorageUploadMode.DIRECT : StorageUploadMode.PROXY;
    }

    @Override
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
        return fileStorage.getFileSize() > runtimeSettings.getDownloadProxyThresholdBytes()
                ? StorageDownloadMode.DIRECT
                : StorageDownloadMode.PROXY;
    }
}
