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

package tech.lamprism.lampray.web.observability.health;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.observability.Health;
import tech.lamprism.lampray.observability.HealthContributor;
import tech.lamprism.lampray.web.observability.management.ManagementExposurePolicy;

import java.util.Map;

/**
 * @author RollW
 */
@Component
public class ObservabilityHealthContributor implements HealthContributor {
    private final ManagementExposurePolicy exposurePolicy;

    public ObservabilityHealthContributor(ManagementExposurePolicy exposurePolicy) {
        this.exposurePolicy = exposurePolicy;
    }

    @Override
    public String getName() {
        return HealthContributorNames.OBSERVABILITY;
    }

    @Override
    public Health health() {
        return Health.up().withDetails(Map.of(
                "exposedEndpoints", exposurePolicy.exposedEndpoints()
        ));
    }
}
