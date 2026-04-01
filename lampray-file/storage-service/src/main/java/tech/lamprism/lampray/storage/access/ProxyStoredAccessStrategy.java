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

package tech.lamprism.lampray.storage.access;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.StorageDownloadMode;
import tech.lamprism.lampray.storage.StorageDownloadResult;
import tech.lamprism.lampray.storage.StorageReference;
import tech.lamprism.lampray.storage.StorageReferenceRequest;
import tech.lamprism.lampray.storage.monitoring.MonitoringStorageDownloadSource;
import tech.lamprism.lampray.storage.monitoring.StorageTrafficPublisher;

import java.io.IOException;

/**
 * Implements proxied access for stored files.
 *
 * @author RollW
 */
@Component
public class ProxyStoredAccessStrategy implements StoredAccessStrategy {
    private final ProxyStorageReferenceFactory proxyStorageReferenceFactory;
    private final StorageTrafficPublisher storageTrafficPublisher;

    public ProxyStoredAccessStrategy(ProxyStorageReferenceFactory proxyStorageReferenceFactory,
                                     StorageTrafficPublisher storageTrafficPublisher) {
        this.proxyStorageReferenceFactory = proxyStorageReferenceFactory;
        this.storageTrafficPublisher = storageTrafficPublisher;
    }

    @Override
    public StorageDownloadMode mode() {
        return StorageDownloadMode.PROXY;
    }

    @Override
    public StorageDownloadResult resolveDownload(StoredDownloadTarget target) throws IOException {
        return new StorageDownloadResult(
                target.getFileStorage(),
                StorageDownloadMode.PROXY,
                null,
                new MonitoringStorageDownloadSource(
                        target.getBlobStore().openDownload(target.getPlacement().getObjectKey()),
                        storageTrafficPublisher,
                        target.getGroupConfig().getName()
                )
        );
    }

    @Override
    public StorageReference resolveReference(String fileId,
                                             StoredDownloadTarget target,
                                             StorageReferenceRequest request) {
        return proxyStorageReferenceFactory.create(fileId);
    }
}
