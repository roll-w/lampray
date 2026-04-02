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

package tech.lamprism.lampray.web.observability.management;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.web.common.keys.ObservabilityConfigKeys;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * @author RollW
 */
@Component
public class ManagementExposurePolicy {
    private static final List<String> DEFAULT_EXPOSED_ENDPOINTS = List.of(
            ManagementEndpointIds.INFO,
            ManagementEndpointIds.HEALTH,
            ManagementEndpointIds.METRICS
    );

    private final ConfigReader configReader;

    public ManagementExposurePolicy(ConfigReader configReader) {
        this.configReader = configReader;
    }

    public boolean isExposed(String endpointId) {
        return exposedEndpoints().contains(normalize(endpointId));
    }

    public Set<String> exposedEndpoints() {
        Set<String> configured = configReader.get(ObservabilityConfigKeys.MANAGEMENT_EXPOSED_ENDPOINTS);
        if (configured == null || configured.isEmpty()) {
            return defaultExposedEndpoints();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : configured) {
            if (value == null) {
                continue;
            }
            String trimmed = normalize(value);
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }

        if (normalized.isEmpty()) {
            return defaultExposedEndpoints();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(normalized));
    }

    private String normalize(String value) {
        Objects.requireNonNull(value, "value cannot be null");
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private Set<String> defaultExposedEndpoints() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(DEFAULT_EXPOSED_ENDPOINTS));
    }
}
