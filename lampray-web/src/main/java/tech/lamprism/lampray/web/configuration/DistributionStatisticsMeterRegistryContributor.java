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

package tech.lamprism.lampray.web.configuration;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.observability.MeterRegistryContributor;

import java.time.Duration;

/**
 * @author RollW
 */
@Component
public class DistributionStatisticsMeterRegistryContributor implements MeterRegistryContributor {
    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public void contribute(MeterRegistry meterRegistry) {
        meterRegistry.config()
                .meterFilter(MeterFilter.maximumAllowableMetrics(5000))
                .meterFilter(new MeterFilter() {
                    @Override
                    public DistributionStatisticConfig configure(Meter.Id id,
                                                                 DistributionStatisticConfig config) {
                        if (supportsDistributionStatistics(id)) {
                            return DistributionStatisticConfig.builder()
                                    .percentilesHistogram(true)
                                    .percentiles(0.5, 0.95, 0.99)
                                    .serviceLevelObjectives(
                                            Duration.ofMillis(100).toNanos(),
                                            Duration.ofMillis(300).toNanos(),
                                            Duration.ofSeconds(1).toNanos()
                                    )
                                    .build()
                                    .merge(config);
                        }
                        return config;
                    }
                });
    }

    private boolean supportsDistributionStatistics(Meter.Id id) {
        String name = id.getName();
        return name.startsWith("lampray.http.server.request")
                || name.startsWith("lampray.async.task");
    }
}
