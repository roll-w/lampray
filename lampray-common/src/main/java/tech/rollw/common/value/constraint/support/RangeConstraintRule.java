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

import tech.rollw.common.value.constraint.ValueConstraintRule;
import tech.rollw.common.value.constraint.ValueValidationResult;

/**
 * Constraint rule to check if a comparable value is within a range.
 *
 * @author RollW
 */
public class RangeConstraintRule<V extends Comparable<V>> implements ValueConstraintRule<V> {
    private static final String TYPE = "range";

    private final V min;
    private final V max;
    private final boolean minInclusive;
    private final boolean maxInclusive;

    public RangeConstraintRule(V min, V max) {
        this(min, max, true, true);
    }

    public RangeConstraintRule(V min, V max, boolean minInclusive, boolean maxInclusive) {
        this.min = min;
        this.max = max;
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public ValueValidationResult validate(V value) {
        if (value == null) {
            return ValueValidationResult.failure("Value cannot be null");
        }

        if (min != null) {
            int minCompare = value.compareTo(min);
            if (minInclusive ? minCompare < 0 : minCompare <= 0) {
                return ValueValidationResult.failure(
                        "Value must be " + (minInclusive ? ">=" : ">") + " " + min
                );
            }
        }

        if (max != null) {
            int maxCompare = value.compareTo(max);
            if (maxInclusive ? maxCompare > 0 : maxCompare >= 0) {
                return ValueValidationResult.failure(
                        "Value must be " + (maxInclusive ? "<=" : "<") + " " + max
                );
            }
        }

        return ValueValidationResult.success();
    }

    @Override
    public Descriptor getDescriptor() {
        return new RangeDescriptor(min, max, minInclusive, maxInclusive);
    }

    public static class RangeDescriptor implements Descriptor {
        private final Object min;
        private final Object max;
        private final boolean minInclusive;
        private final boolean maxInclusive;

        public RangeDescriptor(Object min, Object max, boolean minInclusive, boolean maxInclusive) {
            this.min = min;
            this.max = max;
            this.minInclusive = minInclusive;
            this.maxInclusive = maxInclusive;
        }

        public Object getMin() {
            return min;
        }

        public Object getMax() {
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
            return "Range{" +
                    (minInclusive ? "[" : "(") +
                    min + ", " + max +
                    (maxInclusive ? "]" : ")") +
                    "}";
        }
    }
}

