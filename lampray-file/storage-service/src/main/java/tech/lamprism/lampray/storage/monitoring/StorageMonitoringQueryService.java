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

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.backend.BlobStoreRegistration;
import tech.lamprism.lampray.storage.backend.BlobStoreRegistry;
import tech.lamprism.lampray.storage.configuration.StorageGroupBackend;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageTopology;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author RollW
 */
@Service
public class StorageMonitoringQueryService implements StorageMonitoringService {
    private final StorageTopology storageTopology;
    private final BlobStoreRegistry blobStoreRegistry;
    private final StorageStatisticsEngine storageStatisticsEngine;
    private final StorageTrafficAccumulator storageTrafficAccumulator;

    public StorageMonitoringQueryService(StorageTopology storageTopology,
                                         BlobStoreRegistry blobStoreRegistry,
                                         StorageStatisticsEngine storageStatisticsEngine,
                                         StorageTrafficAccumulator storageTrafficAccumulator) {
        this.storageTopology = storageTopology;
        this.blobStoreRegistry = blobStoreRegistry;
        this.storageStatisticsEngine = storageStatisticsEngine;
        this.storageTrafficAccumulator = storageTrafficAccumulator;
    }

    @Override
    public StorageMonitoringOverview getOverview() {
        StorageOverviewTotals totals = storageStatisticsEngine.loadOverviewTotals(OffsetDateTime.now());
        Map<String, StorageBackendTotals> backendTotals = storageStatisticsEngine.loadBackendTotals();
        Map<String, StorageGroupTotals> groupTotals = storageStatisticsEngine.loadGroupTotals();
        Set<String> backendNames = new LinkedHashSet<>(storageTopology.getBackends().keySet());
        backendNames.addAll(blobStoreRegistry.registrations().stream().map(BlobStoreRegistration::getBackendName).collect(Collectors.toSet()));
        backendNames.addAll(backendTotals.keySet());
        Set<String> groupNames = new LinkedHashSet<>(storageTopology.getGroups().keySet());
        groupNames.addAll(groupTotals.keySet());
        return new StorageMonitoringOverview(
                totals.getFileCount(),
                totals.getLogicalBytes(),
                totals.getBlobCount(),
                totals.getUniqueBytes(),
                totals.getPlacementCount(),
                totals.getPhysicalBytes(),
                backendNames.size(),
                groupNames.size(),
                totals.getSessionCounts(),
                getTrafficSnapshot()
        );
    }

    @Override
    public StorageTrafficSnapshot getTrafficSnapshot() {
        return storageTrafficAccumulator.snapshot();
    }

    @Override
    public StorageTrafficSnapshot getTrafficSnapshotForBackend(String backendName) {
        return storageTrafficAccumulator.snapshotForBackend(backendName);
    }

    @Override
    public StorageTrafficSnapshot getTrafficSnapshotForGroup(String groupName) {
        return storageTrafficAccumulator.snapshotForGroup(groupName);
    }

    @Override
    public List<StorageBackendMonitoringView> listBackendMonitoring() {
        Map<String, BlobStoreRegistration> registrationMap = blobStoreRegistry.registrations().stream()
                .collect(Collectors.toMap(BlobStoreRegistration::getBackendName, registration -> registration, (left, right) -> left, LinkedHashMap::new));
        Set<String> backendNames = new LinkedHashSet<>(storageTopology.getBackends().keySet());
        backendNames.addAll(registrationMap.keySet());
        Map<String, StorageBackendTotals> backendTotals = storageStatisticsEngine.loadBackendTotals();
        backendNames.addAll(backendTotals.keySet());
        List<StorageBackendMonitoringView> result = new ArrayList<>();
        for (String backendName : backendNames) {
            var backendConfig = storageTopology.getBackends().get(backendName);
            BlobStoreRegistration registration = registrationMap.get(backendName);
            StorageBackendTotals totals = backendTotals.getOrDefault(
                    backendName,
                    new StorageBackendTotals(0L, 0L, 0L, 0L)
            );
            result.add(new StorageBackendMonitoringView(
                    backendName,
                    backendConfig == null ? null : backendConfig.getType(),
                    registration != null,
                    registration == null ? Set.of() : registration.getBlobStore().getCapabilities(),
                    registration == null ? Map.of() : registration.getGroupWeights(),
                    totals.getPrimaryBlobCount(),
                    totals.getPlacementCount(),
                    totals.getUniqueBytes(),
                    totals.getPhysicalBytes(),
                    getTrafficSnapshotForBackend(backendName)
            ));
        }
        return result;
    }

    @Override
    public List<StorageGroupMonitoringView> listGroupMonitoring() {
        Map<String, StorageGroupTotals> groupTotals = storageStatisticsEngine.loadGroupTotals();
        Set<String> groupNames = new LinkedHashSet<>(storageTopology.getGroups().keySet());
        groupNames.addAll(groupTotals.keySet());
        List<StorageGroupMonitoringView> result = new ArrayList<>();
        for (String groupName : groupNames) {
            StorageGroupConfig groupConfig = storageTopology.getGroups().get(groupName);
            StorageGroupTotals totals = groupTotals.getOrDefault(
                    groupName,
                    new StorageGroupTotals(0L, 0L, 0L, 0L)
            );
            result.add(new StorageGroupMonitoringView(
                    groupName,
                    groupConfig == null ? null : groupConfig.getMaxSizeBytes(),
                    totals.getFileCount(),
                    totals.getLogicalBytes(),
                    totals.getDistinctBlobCount(),
                    totals.getUniqueBytes(),
                    groupConfig == null ? List.of() : groupConfig.getBackends().stream().map(StorageGroupBackend::getBackendName).toList(),
                    getTrafficSnapshotForGroup(groupName)
            ));
        }
        return result;
    }
}
