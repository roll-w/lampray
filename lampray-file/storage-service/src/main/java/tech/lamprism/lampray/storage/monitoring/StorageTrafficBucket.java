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

import java.util.concurrent.atomic.LongAdder;

/**
 * Aggregates storage traffic counters for a scope.
 *
 * @author RollW
 */
public final class StorageTrafficBucket {
    private final TransferCounter upload = new TransferCounter();
    private final TransferCounter download = new TransferCounter();
    private final RequestCounter directUpload = new RequestCounter();
    private final RequestCounter directDownload = new RequestCounter();

    public void record(StorageTrafficEvent event) {
        switch (event.getType()) {
            case UPLOAD_REQUEST -> upload.incrementRequests();
            case UPLOAD_BYTES -> upload.addBytes(event.getAmount());
            case DOWNLOAD_BYTES -> download.addBytes(event.getAmount());
            case DOWNLOAD_REQUEST -> download.incrementRequests();
            case DIRECT_UPLOAD_REQUEST -> directUpload.add(event.getAmount());
            case DIRECT_DOWNLOAD_REQUEST -> directDownload.incrementCount();
        }
    }

    public StorageTrafficSnapshot snapshot() {
        return new StorageTrafficSnapshot(
                upload.bytes(),
                upload.requests(),
                download.bytes(),
                download.requests(),
                directUpload.count(),
                directUpload.totalAmount(),
                directDownload.count()
        );
    }

    private static final class TransferCounter {
        private final LongAdder bytes = new LongAdder();
        private final LongAdder requests = new LongAdder();

        private void addBytes(long delta) {
            if (delta <= 0) {
                return;
            }
            bytes.add(delta);
        }

        private void incrementRequests() {
            requests.increment();
        }

        private long bytes() {
            return bytes.sum();
        }

        private long requests() {
            return requests.sum();
        }
    }

    private static final class RequestCounter {
        private final LongAdder count = new LongAdder();
        private final LongAdder totalAmount = new LongAdder();

        private void add(long amount) {
            count.increment();
            if (amount > 0) {
                totalAmount.add(amount);
            }
        }

        private void incrementCount() {
            add(0L);
        }

        private long count() {
            return count.sum();
        }

        private long totalAmount() {
            return totalAmount.sum();
        }
    }
}
