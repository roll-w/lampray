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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.observability.Health;
import tech.lamprism.lampray.observability.HealthContributor;
import tech.lamprism.lampray.observability.HealthStatus;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.web.common.keys.ObservabilityConfigKeys;
import tech.lamprism.lampray.web.observability.management.ManagementEndpointIds;
import tech.lamprism.lampray.web.observability.management.ManagementExposurePolicy;

import java.util.Map;

/**
 * @author RollW
 */
@Component
public class PrometheusHealthContributor implements HealthContributor {
    private final MeterRegistry meterRegistry;
    private final ConfigReader configReader;
    private final ManagementExposurePolicy exposurePolicy;

    public PrometheusHealthContributor(MeterRegistry meterRegistry,
                                       ConfigReader configReader,
                                       ManagementExposurePolicy exposurePolicy) {
        this.meterRegistry = meterRegistry;
        this.configReader = configReader;
        this.exposurePolicy = exposurePolicy;
    }

    @Override
    public String getName() {
        return HealthContributorNames.PROMETHEUS;
    }

    @Override
    public Health health() {
        if (!exposurePolicy.isExposed(ManagementEndpointIds.PROMETHEUS)) {
            return Health.up().withDetails(Map.of(
                    "exposed", false,
                    "reason", "Endpoint disabled by config"
            ));
        }

        String scrapeToken = configReader.get(
                ObservabilityConfigKeys.METRICS_SCRAPE_TOKEN,
                ObservabilityConfigKeys.DEFAULT_METRICS_SCRAPE_TOKEN
        );
        if (scrapeToken.trim().isEmpty()) {
            return Health.down().withDetails(Map.of(
                    "exposed", true,
                    "reason", "Scrape token is not configured"
            ));
        }

        boolean available = meterRegistry instanceof PrometheusMeterRegistry;
        return Health.status(available ? HealthStatus.UP : HealthStatus.DOWN)
                .withDetails(Map.of(
                        "exposed", true,
                        "registryType", meterRegistry.getClass().getName()
                ));
    }
}
