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

package tech.lamprism.lampray.storage.monitoring;

import tech.lamprism.lampray.storage.StorageByteRange;
import tech.lamprism.lampray.storage.StorageDownloadSource;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MonitoringStorageDownloadSource implements StorageDownloadSource {
    private final StorageDownloadSource delegate;
    private final StorageTrafficRecorder trafficRecorder;
    private final String groupName;
    private final String backendName;

    public MonitoringStorageDownloadSource(StorageDownloadSource delegate,
                                           StorageTrafficRecorder trafficRecorder,
                                           String groupName,
                                           String backendName) {
        this.delegate = delegate;
        this.trafficRecorder = trafficRecorder;
        this.groupName = groupName;
        this.backendName = backendName;
    }

    @Override
    public InputStream openStream() throws IOException {
        trafficRecorder.recordProxyDownloadRequest(groupName, backendName);
        return wrap(delegate.openStream());
    }

    @Override
    public InputStream openStream(StorageByteRange range) throws IOException {
        trafficRecorder.recordProxyDownloadRequest(groupName, backendName);
        return wrap(delegate.openStream(range));
    }

    private InputStream wrap(InputStream inputStream) {
        return new FilterInputStream(inputStream) {
            @Override
            public int read() throws IOException {
                int read = super.read();
                if (read >= 0) {
                    trafficRecorder.recordProxyDownload(groupName, backendName, 1);
                }
                return read;
            }

            @Override
            public int read(byte[] bytes,
                            int offset,
                            int length) throws IOException {
                int read = super.read(bytes, offset, length);
                if (read > 0) {
                    trafficRecorder.recordProxyDownload(groupName, backendName, read);
                }
                return read;
            }
        };
    }
}
