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

package tech.lamprism.lampray.storage.file;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Routes cleanup work to the concrete backend/object-key targets that need physical deletion.
 *
 * @author RollW
 */
@Component
public class StorageBlobCleanupRouter {
    public StorageBlobCleanupPlan route(StorageBlobEntity blobEntity,
                                        List<StorageBlobPlacementEntity> placements) {
        Map<String, Set<String>> targets = new LinkedHashMap<>();
        addTarget(targets, blobEntity.getPrimaryBackend(), blobEntity.getPrimaryObjectKey());
        for (StorageBlobPlacementEntity placement : placements) {
            addTarget(targets, placement.getBackendName(), placement.getObjectKey());
        }
        return toPlan(targets);
    }

    public StorageBlobCleanupPlan routePlacements(List<StorageBlobPlacementEntity> placements) {
        Map<String, Set<String>> targets = new LinkedHashMap<>();
        for (StorageBlobPlacementEntity placement : placements) {
            addTarget(targets, placement.getBackendName(), placement.getObjectKey());
        }
        return toPlan(targets);
    }

    private StorageBlobCleanupPlan toPlan(Map<String, Set<String>> targets) {
        List<StorageBlobCleanupTarget> cleanupTargets = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : targets.entrySet()) {
            for (String objectKey : entry.getValue()) {
                cleanupTargets.add(new StorageBlobCleanupTarget(entry.getKey(), objectKey));
            }
        }
        return new StorageBlobCleanupPlan(List.copyOf(cleanupTargets));
    }

    private void addTarget(Map<String, Set<String>> targets,
                           String backendName,
                           String objectKey) {
        targets.computeIfAbsent(backendName, ignored -> new LinkedHashSet<>()).add(objectKey);
    }

    public record StorageBlobCleanupTarget(String backendName,
                                           String objectKey) {
    }

    public record StorageBlobCleanupPlan(List<StorageBlobCleanupTarget> targets) {
        public static StorageBlobCleanupPlan empty() {
            return new StorageBlobCleanupPlan(List.of());
        }
    }
}
