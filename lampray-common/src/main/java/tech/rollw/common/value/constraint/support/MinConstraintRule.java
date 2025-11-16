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

/**
 * Constraint rule to check if a comparable value is greater than or equal to a minimum.
 *
 * @author RollW
 */
public class MinConstraintRule<V extends Comparable<V>> implements ValueConstraintRule<V> {
    private static final String TYPE = "min";

    private final V min;
    private final boolean inclusive;

    public MinConstraintRule(V min) {
        this(min, true);
    }

    public MinConstraintRule(V min, boolean inclusive) {
        this.min = min;
        this.inclusive = inclusive;
    }

    @NonNull
    @Override
    public String getType() {
        return TYPE;
    }

    @NonNull
    @Override
    public ValueValidationResult validate(@Nullable V value) {
        if (value == null) {
            return ValueValidationResult.failure("Value cannot be null");
        }

        int compare = value.compareTo(min);
        if (inclusive ? compare < 0 : compare <= 0) {
            return ValueValidationResult.failure(
                    "Value must be " + (inclusive ? ">=" : ">") + " " + min
            );
        }

        return ValueValidationResult.success();
    }

    @NonNull
    @Override
    public Descriptor getDescriptor() {
        return new MinDescriptor(min, inclusive);
    }

    public static class MinDescriptor implements Descriptor {
        private final Object min;
        private final boolean inclusive;

        public MinDescriptor(Object min, boolean inclusive) {
            this.min = min;
            this.inclusive = inclusive;
        }

        public Object getMin() {
            return min;
        }

        public boolean isInclusive() {
            return inclusive;
        }

        @Override
        public String toString() {
            return "Min{" + (inclusive ? ">=" : ">") + min + "}";
        }
    }
}

