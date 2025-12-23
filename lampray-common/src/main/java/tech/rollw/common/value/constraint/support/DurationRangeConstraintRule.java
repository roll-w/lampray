/*
 * Copyright (C) 2023-2025 RollW
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

package tech.rollw.common.value.constraint.support;

import space.lingu.NonNull;
import space.lingu.Nullable;
import tech.rollw.common.value.constraint.ValueConstraintRule;
import tech.rollw.common.value.constraint.ValueValidationResult;

import java.time.Duration;

/**
 * @author RollW
 */
public class DurationRangeConstraintRule implements ValueConstraintRule<Duration> {
    private static final String TYPE = "durationRange";

    private final Duration min;
    private final Duration max;
    private final boolean minInclusive;
    private final boolean maxInclusive;

    public DurationRangeConstraintRule(@Nullable Duration min, @Nullable Duration max) {
        this(min, max, true, true);
    }

    public DurationRangeConstraintRule(@Nullable Duration min, @Nullable Duration max,
                                        boolean minInclusive, boolean maxInclusive) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Min duration must not be greater than max duration");
        }
        this.min = min;
        this.max = max;
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    public static DurationRangeConstraintRule atLeast(Duration min) {
        return new DurationRangeConstraintRule(min, null, true, true);
    }

    public static DurationRangeConstraintRule atMost(Duration max) {
        return new DurationRangeConstraintRule(null, max, true, true);
    }

    public static DurationRangeConstraintRule between(Duration min, Duration max) {
        return new DurationRangeConstraintRule(min, max, true, true);
    }

    @NonNull
    @Override
    public String getType() {
        return TYPE;
    }

    @NonNull
    @Override
    public ValueValidationResult validate(@Nullable Duration value) {
        if (value == null) {
            return ValueValidationResult.failure("Duration cannot be null");
        }

        if (min != null) {
            int compare = value.compareTo(min);
            if (minInclusive ? compare < 0 : compare <= 0) {
                return ValueValidationResult.failure(
                        "Duration must be " + (minInclusive ? ">=" : ">") + " " + formatDuration(min) +
                                ", but was " + formatDuration(value)
                );
            }
        }

        if (max != null) {
            int compare = value.compareTo(max);
            if (maxInclusive ? compare > 0 : compare >= 0) {
                return ValueValidationResult.failure(
                        "Duration must be " + (maxInclusive ? "<=" : "<") + " " + formatDuration(max) +
                                ", but was " + formatDuration(value)
                );
            }
        }

        return ValueValidationResult.success();
    }

    @NonNull
    @Override
    public Descriptor getDescriptor() {
        return new DurationRangeDescriptor(min, max, minInclusive, maxInclusive);
    }

    private String formatDuration(Duration duration) {
        return duration.toString();
    }

    public static class DurationRangeDescriptor implements Descriptor {
        private final Duration min;
        private final Duration max;
        private final boolean minInclusive;
        private final boolean maxInclusive;

        public DurationRangeDescriptor(Duration min, Duration max,
                                       boolean minInclusive, boolean maxInclusive) {
            this.min = min;
            this.max = max;
            this.minInclusive = minInclusive;
            this.maxInclusive = maxInclusive;
        }

        public Duration getMin() {
            return min;
        }

        public Duration getMax() {
            return max;
        }

        public boolean isMinInclusive() {
            return minInclusive;
        }

        public boolean isMaxInclusive() {
            return maxInclusive;
        }

        @Override
        public String toString() {
            return "DurationRange{" +
                    (minInclusive ? "[" : "(") +
                    (min != null ? formatOrNull(min) : "-") + ", " +
                    (max != null ? formatOrNull(max) : "+") +
                    (maxInclusive ? "]" : ")") +
                    "}";
        }

        private String formatOrNull(Duration duration) {
            return duration == null ? "null" : duration.toString();
        }
    }
}
