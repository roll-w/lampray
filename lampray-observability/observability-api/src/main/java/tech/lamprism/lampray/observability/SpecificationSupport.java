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

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
final class SpecificationSupport {
    private SpecificationSupport() {
    }

    static String requireName(String name) {
        return requireText(name, "name");
    }

    static String requireText(String value,
                              String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value;
    }

    static String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }

    static String normalizeOptionalText(String value,
                                        String fieldName) {
        if (value == null) {
            return null;
        }
        return requireText(value, fieldName).trim();
    }

    static List<Double> normalizePercentiles(List<Double> percentiles) {
        Objects.requireNonNull(percentiles, "percentiles cannot be null");
        for (Double percentile : percentiles) {
            Objects.requireNonNull(percentile, "percentile cannot be null");
            if (percentile <= 0D || percentile >= 1D) {
                throw new IllegalArgumentException("Percentiles must be between 0 and 1");
            }
        }
        return List.copyOf(percentiles);
    }

    static List<Duration> normalizeDurations(List<Duration> durations,
                                             String fieldName) {
        Objects.requireNonNull(durations, fieldName + " cannot be null");
        for (Duration duration : durations) {
            Objects.requireNonNull(duration, fieldName + " cannot contain null values");
            if (duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException(fieldName + " must contain positive durations");
            }
        }
        return List.copyOf(durations);
    }

    static List<Double> normalizePositiveNumbers(List<Double> values,
                                                 String fieldName) {
        Objects.requireNonNull(values, fieldName + " cannot be null");
        for (Double value : values) {
            Objects.requireNonNull(value, fieldName + " cannot contain null values");
            if (value <= 0D) {
                throw new IllegalArgumentException(fieldName + " must contain positive values");
            }
        }
        return List.copyOf(values);
    }
}
