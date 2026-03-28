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

@Component
public class StorageTrafficAccumulator implements StorageMonitoringListener {
    private final StorageTrafficBucket overall = new StorageTrafficBucket();
    private final ConcurrentMap<StorageTrafficScope, StorageTrafficBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void onEvent(StorageTrafficEvent event) {
        if (event.getAmount() < 0) {
            return;
        }
        if (event.getScope().getType() == StorageTrafficScopeType.OVERALL) {
            overall.record(event);
            return;
        }
        bucketFor(event.getScope()).record(event);
    }

    public StorageTrafficSnapshot snapshot() {
        return overall.snapshot();
    }

    public StorageTrafficSnapshot snapshotForGroup(String groupName) {
        return snapshotOf(buckets.get(StorageTrafficScope.group(groupName)));
    }

    public StorageTrafficSnapshot snapshotForBackend(String backendName) {
        return snapshotOf(buckets.get(StorageTrafficScope.backend(backendName)));
    }

    private StorageTrafficSnapshot snapshotOf(StorageTrafficBucket bucket) {
        return bucket == null ? StorageTrafficSnapshot.empty() : bucket.snapshot();
    }

    private StorageTrafficBucket bucketFor(StorageTrafficScope scope) {
        return buckets.computeIfAbsent(scope, ignored -> new StorageTrafficBucket());
    }
}
