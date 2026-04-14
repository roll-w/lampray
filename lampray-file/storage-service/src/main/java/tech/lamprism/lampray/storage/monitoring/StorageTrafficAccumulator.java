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
import tech.lamprism.lampray.storage.backend.BlobStoreRegistry;
import tech.lamprism.lampray.storage.configuration.StorageTopology;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author RollW
 */
@Component
public class StorageTrafficAccumulator implements StorageMonitoringListener, StorageLiveTrafficService {
    private final StorageTrafficBucket overall = new StorageTrafficBucket();
    private final ConcurrentMap<StorageTrafficScope, StorageTrafficBucket> buckets = new ConcurrentHashMap<>();
    private final StorageTopology storageTopology;
    private final BlobStoreRegistry blobStoreRegistry;

    public StorageTrafficAccumulator(StorageTopology storageTopology,
                                    BlobStoreRegistry blobStoreRegistry) {
        this.storageTopology = storageTopology;
        this.blobStoreRegistry = blobStoreRegistry;
    }

    @Override
    public void onEvent(StorageTrafficEvent event) {
        if (event.getAmount() < 0) {
            return;
        }
        if (event.getScope().getType() == StorageTrafficScopeType.OVERALL) {
            overall.record(event);
            return;
        }
        StorageTrafficBucket bucket = bucketFor(event.getScope());
        if (bucket != null) {
            bucket.record(event);
        }
    }

    @Override
    public StorageTrafficSnapshot snapshotOverall() {
        pruneStaleScopes();
        return overall.snapshot();
    }

    @Override
    public StorageTrafficSnapshot snapshotForGroup(String groupName) {
        pruneStaleScopes();
        return snapshotOf(buckets.get(StorageTrafficScope.group(groupName)));
    }

    @Override
    public StorageTrafficSnapshot snapshotForBackend(String backendName) {
        pruneStaleScopes();
        return snapshotOf(buckets.get(StorageTrafficScope.backend(backendName)));
    }

    private StorageTrafficSnapshot snapshotOf(StorageTrafficBucket bucket) {
        return bucket == null ? StorageTrafficSnapshot.empty() : bucket.snapshot();
    }

    private StorageTrafficBucket bucketFor(StorageTrafficScope scope) {
        if (!supportsScope(scope)) {
            return null;
        }
        return buckets.computeIfAbsent(scope, ignored -> new StorageTrafficBucket());
    }

    private boolean supportsScope(StorageTrafficScope scope) {
        if (scope.getType() == StorageTrafficScopeType.OVERALL) {
            return true;
        }
        if (scope.getType() == StorageTrafficScopeType.GROUP) {
            return storageTopology.getGroups().containsKey(scope.getName());
        }
        return storageTopology.getBackends().containsKey(scope.getName()) || blobStoreRegistry.contains(scope.getName());
    }

    private void pruneStaleScopes() {
        List<StorageTrafficScope> staleScopes = new ArrayList<>();
        for (StorageTrafficScope scope : buckets.keySet()) {
            if (!supportsScope(scope)) {
                staleScopes.add(scope);
            }
        }
        for (StorageTrafficScope scope : staleScopes) {
            buckets.remove(scope);
        }
    }
}
