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

package tech.lamprism.lampray.observability.core;

import tech.lamprism.lampray.observability.Health;
import tech.lamprism.lampray.observability.HealthContributor;
import tech.lamprism.lampray.observability.HealthGroupSpecification;
import tech.lamprism.lampray.observability.HealthStatus;
import tech.lamprism.lampray.observability.StatusAggregator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author RollW
 */
public final class CompositeHealthService {
    private final Map<String, HealthContributor> contributors;
    private final StatusAggregator statusAggregator;

    public CompositeHealthService(List<HealthContributor> contributors,
                                  StatusAggregator statusAggregator) {
        Objects.requireNonNull(contributors, "contributors cannot be null");
        this.statusAggregator = Objects.requireNonNull(statusAggregator, "statusAggregator cannot be null");
        this.contributors = new LinkedHashMap<>();
        for (HealthContributor contributor : contributors) {
            HealthContributor existing = this.contributors.putIfAbsent(
                    contributor.getName(),
                    contributor
            );
            if (existing != null) {
                throw new IllegalArgumentException("Duplicate health contributor: " + contributor.getName());
            }
        }
    }

    public Health group(HealthGroupSpecification specification) {
        Objects.requireNonNull(specification, "specification cannot be null");

        LinkedHashMap<String, Health> components = new LinkedHashMap<>();
        ArrayList<HealthStatus> statuses = new ArrayList<>();
        for (String contributorName : specification.getContributorNames()) {
            Health contributorHealth = resolveContributor(contributorName);
            components.put(contributorName, contributorHealth);
            statuses.add(contributorHealth.statusValue());
        }

        Health health = Health.status(statusAggregator.aggregate(statuses));
        if (components.isEmpty()) {
            return health.withDetail("reason", "No health contributors configured for group");
        }
        return health.withComponents(components);
    }

    public Health overall(List<HealthGroupSpecification> specifications) {
        Objects.requireNonNull(specifications, "specifications cannot be null");

        LinkedHashMap<String, Health> groups = new LinkedHashMap<>();
        ArrayList<HealthStatus> statuses = new ArrayList<>();
        for (HealthGroupSpecification specification : specifications) {
            Health groupHealth = group(specification);
            groups.put(specification.getName(), groupHealth);
            statuses.add(groupHealth.statusValue());
        }

        Health health = Health.status(statusAggregator.aggregate(statuses));
        if (groups.isEmpty()) {
            return health.withDetail("reason", "No health groups configured");
        }
        return health.withComponents(groups);
    }

    private Health resolveContributor(String contributorName) {
        HealthContributor contributor = contributors.get(contributorName);
        if (contributor == null) {
            return Health.down().withDetail("reason", "Unknown health contributor");
        }
        return contributor.health();
    }
}
