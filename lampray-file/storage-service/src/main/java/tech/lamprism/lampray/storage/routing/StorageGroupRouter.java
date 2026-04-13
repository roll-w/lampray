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

import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves read and write routing for storage groups.
 *
 * @author RollW
 */
public interface StorageGroupRouter {
    /**
     * Selects the write plan for a group.
     */
    StorageWritePlan selectWritePlan(String groupName);

    /**
     * Restores the write plan for an existing session or object.
     */
    StorageWritePlan restoreWritePlan(String groupName,
                                      String primaryBackend);

    /**
     * Selects the backend used for reads.
     */
    String selectReadBackend(String groupName,
                             Collection<String> availableBackends);

    /**
     * Lists currently active backends for the group.
     */
    List<String> resolveActiveBackends(String groupName);

    default StorageBlobCleanupPlan routeCleanup(StorageBlobEntity blobEntity,
                                                List<StorageBlobPlacementEntity> placements) {
        Map<String, Set<String>> targets = new LinkedHashMap<>();
        addCleanupTarget(targets, blobEntity.getPrimaryBackend(), blobEntity.getPrimaryObjectKey());
        for (StorageBlobPlacementEntity placement : placements) {
            addCleanupTarget(targets, placement.getBackendName(), placement.getObjectKey());
        }
        return toCleanupPlan(targets);
    }

    default StorageBlobCleanupPlan routeCleanup(List<StorageBlobPlacementEntity> placements) {
        Map<String, Set<String>> targets = new LinkedHashMap<>();
        for (StorageBlobPlacementEntity placement : placements) {
            addCleanupTarget(targets, placement.getBackendName(), placement.getObjectKey());
        }
        return toCleanupPlan(targets);
    }

    private static void addCleanupTarget(Map<String, Set<String>> targets,
                                         String backendName,
                                         String objectKey) {
        targets.computeIfAbsent(backendName, ignored -> new LinkedHashSet<>()).add(objectKey);
    }

    private static StorageBlobCleanupPlan toCleanupPlan(Map<String, Set<String>> targets) {
        List<StorageBlobCleanupTarget> cleanupTargets = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : targets.entrySet()) {
            for (String objectKey : entry.getValue()) {
                cleanupTargets.add(new StorageBlobCleanupTarget(entry.getKey(), objectKey));
            }
        }
        return new StorageBlobCleanupPlan(List.copyOf(cleanupTargets));
    }

    record StorageBlobCleanupTarget(String backendName,
                                    String objectKey) {
    }

    record StorageBlobCleanupPlan(List<StorageBlobCleanupTarget> targets) {
        public static StorageBlobCleanupPlan empty() {
            return new StorageBlobCleanupPlan(List.of());
        }
    }
}
