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

package tech.lamprism.lampray.storage.facade;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.StorageBackendType;
import tech.lamprism.lampray.storage.backend.BlobStoreRegistration;
import tech.lamprism.lampray.storage.backend.BlobStoreRegistry;
import tech.lamprism.lampray.storage.configuration.StorageBackendConfig;
import tech.lamprism.lampray.storage.configuration.StorageGroupBackend;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageTopology;
import tech.lamprism.lampray.storage.monitoring.StorageBackendTotals;
import tech.lamprism.lampray.storage.monitoring.StorageGroupTotals;
import tech.lamprism.lampray.storage.monitoring.StorageStatisticsEngine;
import tech.lamprism.lampray.storage.query.StorageBackendView;
import tech.lamprism.lampray.storage.query.StorageGroupView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StorageTopologyQueryService {
    private final StorageTopology storageTopology;
    private final BlobStoreRegistry blobStoreRegistry;
    private final StorageStatisticsEngine storageStatisticsEngine;

    public StorageTopologyQueryService(StorageTopology storageTopology,
                                       BlobStoreRegistry blobStoreRegistry,
                                       StorageStatisticsEngine storageStatisticsEngine) {
        this.storageTopology = storageTopology;
        this.blobStoreRegistry = blobStoreRegistry;
        this.storageStatisticsEngine = storageStatisticsEngine;
    }

    public List<StorageBackendView> listBackends() {
        Map<String, BlobStoreRegistration> registrationMap = blobStoreRegistry.registrations().stream()
                .collect(Collectors.toMap(BlobStoreRegistration::getBackendName, registration -> registration, (left, right) -> left, LinkedHashMap::new));
        Map<String, StorageBackendTotals> backendTotals = storageStatisticsEngine.loadBackendTotals();
        Set<String> backendNames = new java.util.LinkedHashSet<>(storageTopology.getBackends().keySet());
        backendNames.addAll(registrationMap.keySet());
        backendNames.addAll(backendTotals.keySet());

        List<StorageBackendView> result = new ArrayList<>();
        for (String backendName : backendNames) {
            StorageBackendConfig config = storageTopology.getBackends().get(backendName);
            BlobStoreRegistration registration = registrationMap.get(backendName);
            StorageBackendType backendType = config != null ? config.getType() : null;
            StorageBackendTotals totals = backendTotals.getOrDefault(
                    backendName,
                    new StorageBackendTotals(0L, 0L, 0L, 0L)
            );
            result.add(new StorageBackendView(
                    backendName,
                    backendType,
                    registration != null,
                    registration == null ? Set.of() : registration.getBlobStore().getCapabilities(),
                    registration == null ? Map.of() : registration.getGroupWeights(),
                    config == null ? null : config.getEndpoint(),
                    config == null ? null : config.getPublicEndpoint(),
                    config == null ? null : config.getRegion(),
                    config == null ? null : config.getBucket(),
                    config == null ? null : config.getRootPrefix(),
                    config == null ? null : config.getRootPath(),
                    config != null && config.getNativeChecksumEnabled(),
                    config != null && config.getPathStyleAccess(),
                    totals.getPrimaryBlobCount(),
                    totals.getPlacementCount(),
                    totals.getUniqueBytes(),
                    totals.getPhysicalBytes()
            ));
        }
        return result;
    }

    public List<StorageGroupView> listGroups() {
        Map<String, StorageGroupTotals> groupTotals = storageStatisticsEngine.loadGroupTotals();
        Set<String> groupNames = new java.util.LinkedHashSet<>(storageTopology.getGroups().keySet());
        groupNames.addAll(groupTotals.keySet());
        List<StorageGroupView> result = new ArrayList<>();
        for (String groupName : groupNames) {
            StorageGroupConfig groupConfig = storageTopology.getGroups().get(groupName);
            StorageGroupTotals totals = groupTotals.getOrDefault(
                    groupName,
                    new StorageGroupTotals(0L, 0L, 0L, 0L)
            );
            result.add(new StorageGroupView(
                    groupName,
                    groupConfig == null ? null : groupConfig.getVisibility(),
                    groupConfig == null ? null : groupConfig.getDownloadPolicy(),
                    groupConfig == null ? null : groupConfig.getPlacementMode(),
                    groupConfig == null ? null : groupConfig.getLoadBalanceMode(),
                    groupConfig == null ? null : groupConfig.getMaxSizeBytes(),
                    groupConfig == null ? Set.of() : groupConfig.getAllowedFileTypes(),
                    groupConfig == null ? List.of() : groupConfig.getBackends().stream().map(StorageGroupBackend::getBackendName).toList(),
                    totals.getFileCount(),
                    totals.getLogicalBytes(),
                    totals.getDistinctBlobCount(),
                    totals.getUniqueBytes()
            ));
        }
        return result;
    }
}
