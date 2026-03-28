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

package tech.lamprism.lampray.storage.routing;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.configuration.StorageGroupBackend;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageGroupLoadBalanceMode;
import tech.lamprism.lampray.storage.configuration.StorageGroupPlacementMode;
import tech.lamprism.lampray.storage.configuration.StorageTopology;
import tech.lamprism.lampray.storage.backend.BlobStoreRegistration;
import tech.lamprism.lampray.storage.backend.BlobStoreRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author RollW
 */
@Component
public class TopologyStorageGroupRouter implements StorageGroupRouter {
    private final StorageTopology storageTopology;
    private final BlobStoreRegistry blobStoreRegistry;
    private final Map<String, AtomicInteger> cursors = new ConcurrentHashMap<>();

    public TopologyStorageGroupRouter(StorageTopology storageTopology,
                                      BlobStoreRegistry blobStoreRegistry) {
        this.storageTopology = storageTopology;
        this.blobStoreRegistry = blobStoreRegistry;
    }

    @Override
    public StorageWritePlan selectWritePlan(String groupName) {
        StorageGroupConfig groupConfig = storageTopology.getGroup(groupName);
        List<StorageGroupBackend> activeBackends = resolveActiveGroupBackends(groupConfig);
        StorageGroupBackend primaryBackend = selectMember(groupName + "#write", activeBackends,
                groupConfig.getLoadBalanceMode());
        return restoreWritePlan(groupConfig, activeBackends, primaryBackend.getBackendName());
    }

    @Override
    public StorageWritePlan restoreWritePlan(String groupName,
                                             String primaryBackend) {
        StorageGroupConfig groupConfig = storageTopology.getGroup(groupName);
        return restoreWritePlan(groupConfig, resolveAvailableGroupBackends(groupConfig), primaryBackend);
    }

    @Override
    public String selectReadBackend(String groupName,
                                    Collection<String> availableBackends) {
        if (availableBackends.isEmpty()) {
            throw new IllegalArgumentException("No available backends for group: " + groupName);
        }
        StorageGroupConfig groupConfig = storageTopology.getGroup(groupName);
        List<StorageGroupBackend> candidates = resolveActiveGroupBackends(groupConfig).stream()
                .filter(backend -> availableBackends.contains(backend.getBackendName()))
                .toList();
        if (candidates.isEmpty()) {
            List<String> activeAvailableBackends = availableBackends.stream()
                    .filter(blobStoreRegistry::contains)
                    .toList();
            if (activeAvailableBackends.isEmpty()) {
                throw new IllegalStateException("No active blob store backends available for group: " + groupName);
            }
            return activeAvailableBackends.get(0);
        }
        return selectMember(groupName + "#read", candidates, groupConfig.getLoadBalanceMode()).getBackendName();
    }

    @Override
    public List<String> resolveActiveBackends(String groupName) {
        return resolveActiveGroupBackends(storageTopology.getGroup(groupName)).stream()
                .map(StorageGroupBackend::getBackendName)
                .toList();
    }

    private List<StorageGroupBackend> resolveActiveGroupBackends(StorageGroupConfig groupConfig) {
        List<StorageGroupBackend> availableBackends = resolveAvailableGroupBackends(groupConfig);
        if (availableBackends.isEmpty()) {
            throw new IllegalStateException("No active blob store backends available for group: " + groupConfig.getName());
        }
        return availableBackends;
    }

    private List<StorageGroupBackend> resolveAvailableGroupBackends(StorageGroupConfig groupConfig) {
        Map<String, Integer> weights = new LinkedHashMap<>();
        for (StorageGroupBackend backend : groupConfig.getBackends()) {
            if (blobStoreRegistry.contains(backend.getBackendName())) {
                weights.putIfAbsent(backend.getBackendName(), backend.getWeight());
            }
        }
        for (BlobStoreRegistration registration : blobStoreRegistry.registrations()) {
            Integer weight = registration.getGroupWeights().get(groupConfig.getName());
            if (weight == null || weight <= 0) {
                continue;
            }
            weights.putIfAbsent(registration.getBackendName(), weight);
        }
        return weights.entrySet().stream()
                .map(entry -> new StorageGroupBackend(entry.getKey(), entry.getValue()))
                .toList();
    }

    private StorageGroupBackend selectMember(String cursorKey,
                                             List<StorageGroupBackend> backends,
                                             StorageGroupLoadBalanceMode loadBalanceMode) {
        if (backends.size() == 1 || loadBalanceMode == StorageGroupLoadBalanceMode.ORDERED) {
            return backends.get(0);
        }
        List<StorageGroupBackend> expanded = expandByWeight(backends);
        AtomicInteger cursor = cursors.computeIfAbsent(cursorKey, ignored -> new AtomicInteger());
        int index = Math.floorMod(cursor.getAndIncrement(), expanded.size());
        return expanded.get(index);
    }

    private List<StorageGroupBackend> expandByWeight(List<StorageGroupBackend> backends) {
        List<StorageGroupBackend> expanded = new ArrayList<>();
        for (StorageGroupBackend backend : backends) {
            for (int i = 0; i < backend.getWeight(); i++) {
                expanded.add(backend);
            }
        }
        return expanded.isEmpty() ? backends : List.copyOf(expanded);
    }

    private StorageWritePlan restoreWritePlan(StorageGroupConfig groupConfig,
                                              List<StorageGroupBackend> activeBackends,
                                              String primaryBackend) {
        List<String> mirrors = groupConfig.getPlacementMode() == StorageGroupPlacementMode.MIRROR
                ? activeBackends.stream()
                .map(StorageGroupBackend::getBackendName)
                .filter(candidate -> !candidate.equals(primaryBackend))
                .toList()
                : List.of();
        return new StorageWritePlan(groupConfig, primaryBackend, mirrors);
    }
}
