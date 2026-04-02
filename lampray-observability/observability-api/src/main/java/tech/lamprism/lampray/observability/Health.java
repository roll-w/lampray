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

package tech.lamprism.lampray.observability;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author RollW
 */
public final class Health {
    private final HealthStatus status;
    private final Map<String, Object> details;
    private final Map<String, Health> components;

    private Health(HealthStatus status,
                   Map<String, Object> details,
                   Map<String, Health> components) {
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.details = Collections.unmodifiableMap(new LinkedHashMap<>(details));
        this.components = Collections.unmodifiableMap(new LinkedHashMap<>(components));
    }

    public static Health up() {
        return status(HealthStatus.UP);
    }

    public static Health down() {
        return status(HealthStatus.DOWN);
    }

    public static Health unknown() {
        return status(HealthStatus.UNKNOWN);
    }

    public static Health outOfService() {
        return status(HealthStatus.OUT_OF_SERVICE);
    }

    public static Health status(HealthStatus status) {
        return new Health(status, Map.of(), Map.of());
    }

    public String getStatus() {
        return status.getCode();
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public Map<String, Health> getComponents() {
        return components;
    }

    public HealthStatus statusValue() {
        return status;
    }

    public Health withDetail(String name,
                             Object value) {
        Objects.requireNonNull(name, "name cannot be null");
        Map<String, Object> next = new LinkedHashMap<>(details);
        next.put(name, value);
        return new Health(status, next, components);
    }

    public Health withDetails(Map<String, ?> values) {
        Objects.requireNonNull(values, "values cannot be null");
        Map<String, Object> next = new LinkedHashMap<>(details);
        next.putAll(values);
        return new Health(status, next, components);
    }

    public Health withComponent(String name,
                                Health health) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(health, "health cannot be null");
        Map<String, Health> next = new LinkedHashMap<>(components);
        next.put(name, health);
        return new Health(status, details, next);
    }

    public Health withComponents(Map<String, Health> values) {
        Objects.requireNonNull(values, "values cannot be null");
        Map<String, Health> next = new LinkedHashMap<>(components);
        next.putAll(values);
        return new Health(status, details, next);
    }
}
