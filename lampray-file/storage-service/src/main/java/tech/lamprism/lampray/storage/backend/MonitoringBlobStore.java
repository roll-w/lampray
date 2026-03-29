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

package tech.lamprism.lampray.storage.backend;

import tech.lamprism.lampray.storage.StorageAccessRequest;
import tech.lamprism.lampray.storage.StorageDownloadSource;
import tech.lamprism.lampray.storage.monitoring.BackendMonitoringDownloadSource;
import tech.lamprism.lampray.storage.monitoring.CountingInputStream;
import tech.lamprism.lampray.storage.monitoring.StorageTrafficPublisher;
import tech.lamprism.lampray.storage.store.BlobDownloadRequest;
import tech.lamprism.lampray.storage.store.BlobObject;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.store.BlobStoreCapability;
import tech.lamprism.lampray.storage.store.BlobWriteRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Set;

/**
 * @author RollW
 */
public class MonitoringBlobStore implements BlobStore, AutoCloseable {
    private final BlobStore delegate;
    private final StorageTrafficPublisher trafficPublisher;

    public MonitoringBlobStore(BlobStore delegate,
                               StorageTrafficPublisher trafficPublisher) {
        this.delegate = delegate;
        this.trafficPublisher = trafficPublisher;
    }

    @Override
    public String getBackendName() {
        return delegate.getBackendName();
    }

    @Override
    public Set<BlobStoreCapability> getCapabilities() {
        return delegate.getCapabilities();
    }

    @Override
    public BlobObject store(BlobWriteRequest request,
                            InputStream inputStream) throws IOException {
        trafficPublisher.publishBackendUploadRequest(getBackendName());
        return delegate.store(
                request,
                new CountingInputStream(inputStream, bytes -> trafficPublisher.publishBackendUpload(getBackendName(), bytes))
        );
    }

    @Override
    public StorageDownloadSource openDownload(String key) throws IOException {
        return new BackendMonitoringDownloadSource(delegate.openDownload(key), trafficPublisher, getBackendName());
    }

    @Override
    public BlobObject describe(String key) throws IOException {
        return delegate.describe(key);
    }

    @Override
    public boolean exists(String key) throws IOException {
        return delegate.exists(key);
    }

    @Override
    public boolean delete(String key) throws IOException {
        return delegate.delete(key);
    }

    @Override
    public StorageAccessRequest createDirectUpload(BlobWriteRequest request,
                                                   Duration duration) throws IOException {
        return delegate.createDirectUpload(request, duration);
    }

    @Override
    public StorageAccessRequest createDirectDownload(BlobDownloadRequest request,
                                                     Duration duration) throws IOException {
        return delegate.createDirectDownload(request, duration);
    }

    @Override
    public String createPublicDownloadUrl(BlobDownloadRequest request) throws IOException {
        return delegate.createPublicDownloadUrl(request);
    }

    @Override
    public void close() throws Exception {
        if (delegate instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }
}
