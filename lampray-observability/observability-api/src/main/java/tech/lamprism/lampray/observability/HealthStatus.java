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

import java.util.Locale;
import java.util.Objects;

/**
 * @author RollW
 */
public final class HealthStatus {
    public static final HealthStatus UP = new HealthStatus("UP");
    public static final HealthStatus DOWN = new HealthStatus("DOWN");
    public static final HealthStatus OUT_OF_SERVICE = new HealthStatus("OUT_OF_SERVICE");
    public static final HealthStatus UNKNOWN = new HealthStatus("UNKNOWN");

    private final String code;

    private HealthStatus(String code) {
        this.code = normalize(code);
    }

    public static HealthStatus of(String code) {
        String normalized = normalize(code);
        return switch (normalized) {
            case "UP" -> UP;
            case "DOWN" -> DOWN;
            case "OUT_OF_SERVICE" -> OUT_OF_SERVICE;
            case "UNKNOWN" -> UNKNOWN;
            default -> new HealthStatus(normalized);
        };
    }

    public String getCode() {
        return code;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof HealthStatus status)) {
            return false;
        }
        return Objects.equals(code, status.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return code;
    }

    private static String normalize(String code) {
        Objects.requireNonNull(code, "code cannot be null");
        String normalized = code.trim().replace('-', '_');
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("health status code cannot be blank");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }
}
