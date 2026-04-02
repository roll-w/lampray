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
import tech.lamprism.lampray.observability.HealthGroupSpecification;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.setting.SettingSpecification;
import tech.lamprism.lampray.web.common.keys.ObservabilityConfigKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * @author RollW
 */
@Component
public class ObservabilityHealthGroups {
    private static final List<String> DEFAULT_LIVENESS = List.of(
            HealthContributorNames.APPLICATION,
            HealthContributorNames.OBSERVATION_REGISTRY,
            HealthContributorNames.METER_REGISTRY
    );
    private static final List<String> DEFAULT_READINESS = List.of(
            HealthContributorNames.OBSERVABILITY,
            HealthContributorNames.DATABASE,
            HealthContributorNames.PROMETHEUS
    );

    private final ConfigReader configReader;

    public ObservabilityHealthGroups(ConfigReader configReader) {
        this.configReader = configReader;
    }

    public List<HealthGroupSpecification> all() {
        return List.of(liveness(), readiness());
    }

    public HealthGroupSpecification group(String name) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "liveness" -> liveness();
            case "readiness" -> readiness();
            default -> null;
        };
    }

    public HealthGroupSpecification liveness() {
        return new HealthGroupSpecification("liveness", read(ObservabilityConfigKeys.HEALTH_LIVENESS_CONTRIBUTORS, DEFAULT_LIVENESS));
    }

    public HealthGroupSpecification readiness() {
        return new HealthGroupSpecification("readiness", read(ObservabilityConfigKeys.HEALTH_READINESS_CONTRIBUTORS, DEFAULT_READINESS));
    }

    private List<String> read(SettingSpecification<Set<String>, String> specification,
                              List<String> fallback) {
        Set<String> configured = configReader.get(specification);
        if (configured == null || configured.isEmpty()) {
            return fallback;
        }

        ArrayList<String> values = new ArrayList<>();
        for (String value : configured) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values.isEmpty() ? fallback : List.copyOf(values);
    }
}
