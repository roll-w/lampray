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
 * Constraint rule to check if a string's length is within a range.
 *
 * @author RollW
 */
public class LengthConstraintRule implements ValueConstraintRule<String> {
    private static final String TYPE = "length";

    private final Integer min;
    private final Integer max;

    public LengthConstraintRule(Integer min, Integer max) {
        this.min = min;
        this.max = max;
    }

    public static LengthConstraintRule min(int min) {
        return new LengthConstraintRule(min, null);
    }

    public static LengthConstraintRule max(int max) {
        return new LengthConstraintRule(null, max);
    }

    public static LengthConstraintRule range(int min, int max) {
        return new LengthConstraintRule(min, max);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public ValueValidationResult validate(String value) {
        if (value == null) {
            return ValueValidationResult.failure("Value cannot be null");
        }

        int length = value.length();

        if (min != null && length < min) {
            return ValueValidationResult.failure(
                    "String length must be at least " + min + ", but was " + length
            );
        }

        if (max != null && length > max) {
            return ValueValidationResult.failure(
                    "String length must be at most " + max + ", but was " + length
            );
        }

        return ValueValidationResult.success();
    }

    @Override
    public Descriptor getDescriptor() {
        return new LengthDescriptor(min, max);
    }

    public static class LengthDescriptor implements Descriptor {
        private final Integer min;
        private final Integer max;

        public LengthDescriptor(Integer min, Integer max) {
            this.min = min;
            this.max = max;
        }

        public Integer getMin() {
            return min;
        }

        public Integer getMax() {
            return max;
        }

        @Override
        public String toString() {
            if (min != null && max != null) {
                return "Length{" + min + ".." + max + "}";
            } else if (min != null) {
                return "Length{>=" + min + "}";
            } else if (max != null) {
                return "Length{<=" + max + "}";
            } else {
                return "Length{}";
            }
        }
    }
}

