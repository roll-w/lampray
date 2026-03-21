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

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

@Component
public class StorageTrafficRecorder {
    private final TrafficAccumulator overall = new TrafficAccumulator();
    private final ConcurrentMap<String, TrafficAccumulator> groupAccumulators = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TrafficAccumulator> backendAccumulators = new ConcurrentHashMap<>();

    public void recordProxyUpload(String groupName,
                                  String backendName,
                                  long bytes) {
        if (bytes <= 0) {
            return;
        }
        accumulatorFor(groupAccumulators, groupName).recordUpload(bytes);
    }

    public void recordProxyDownload(String groupName,
                                    String backendName,
                                    long bytes) {
        if (bytes <= 0) {
            return;
        }
        accumulatorFor(groupAccumulators, groupName).recordDownloadBytes(bytes);
    }

    public void recordProxyDownloadRequest(String groupName,
                                           String backendName) {
        accumulatorFor(groupAccumulators, groupName).recordDownloadRequest();
    }

    public void recordDirectUploadRequest(String groupName,
                                          String backendName,
                                          long declaredBytes) {
        overall.recordDirectUploadRequest(declaredBytes);
        accumulatorFor(groupAccumulators, groupName).recordDirectUploadRequest(declaredBytes);
        accumulatorFor(backendAccumulators, backendName).recordDirectUploadRequest(declaredBytes);
    }

    public void recordDirectDownloadRequest(String groupName,
                                            String backendName) {
        overall.recordDirectDownloadRequest();
        accumulatorFor(groupAccumulators, groupName).recordDirectDownloadRequest();
        accumulatorFor(backendAccumulators, backendName).recordDirectDownloadRequest();
    }

    public void recordBackendUpload(String backendName,
                                    long bytes) {
        if (bytes <= 0) {
            return;
        }
        overall.recordUpload(bytes);
        accumulatorFor(backendAccumulators, backendName).recordUpload(bytes);
    }

    public void recordBackendDownloadRequest(String backendName) {
        overall.recordDownloadRequest();
        accumulatorFor(backendAccumulators, backendName).recordDownloadRequest();
    }

    public void recordBackendDownload(String backendName,
                                      long bytes) {
        if (bytes <= 0) {
            return;
        }
        overall.recordDownloadBytes(bytes);
        accumulatorFor(backendAccumulators, backendName).recordDownloadBytes(bytes);
    }

    public StorageTrafficSnapshot snapshot() {
        return overall.snapshot();
    }

    public StorageTrafficSnapshot snapshotForGroup(String groupName) {
        return snapshotOf(groupAccumulators.get(groupName));
    }

    public StorageTrafficSnapshot snapshotForBackend(String backendName) {
        return snapshotOf(backendAccumulators.get(backendName));
    }

    private StorageTrafficSnapshot snapshotOf(TrafficAccumulator accumulator) {
        return accumulator == null ? StorageTrafficSnapshot.empty() : accumulator.snapshot();
    }

    private TrafficAccumulator accumulatorFor(ConcurrentMap<String, TrafficAccumulator> accumulators,
                                             String key) {
        return accumulators.computeIfAbsent(key == null ? "" : key, ignored -> new TrafficAccumulator());
    }

    private static final class TrafficAccumulator {
        private final LongAdder uploadBytes = new LongAdder();
        private final LongAdder uploadCount = new LongAdder();
        private final LongAdder downloadBytes = new LongAdder();
        private final LongAdder downloadCount = new LongAdder();
        private final LongAdder directUploadRequestCount = new LongAdder();
        private final LongAdder directUploadDeclaredBytes = new LongAdder();
        private final LongAdder directDownloadRequestCount = new LongAdder();

        private void recordUpload(long bytes) {
            uploadBytes.add(bytes);
            uploadCount.increment();
        }

        private void recordDownloadBytes(long bytes) {
            downloadBytes.add(bytes);
        }

        private void recordDownloadRequest() {
            downloadCount.increment();
        }

        private void recordDirectUploadRequest(long declaredBytes) {
            directUploadRequestCount.increment();
            if (declaredBytes > 0) {
                directUploadDeclaredBytes.add(declaredBytes);
            }
        }

        private void recordDirectDownloadRequest() {
            directDownloadRequestCount.increment();
        }

        private StorageTrafficSnapshot snapshot() {
            return new StorageTrafficSnapshot(
                    uploadBytes.sum(),
                    uploadCount.sum(),
                    downloadBytes.sum(),
                    downloadCount.sum(),
                    directUploadRequestCount.sum(),
                    directUploadDeclaredBytes.sum(),
                    directDownloadRequestCount.sum()
            );
        }
    }
}
