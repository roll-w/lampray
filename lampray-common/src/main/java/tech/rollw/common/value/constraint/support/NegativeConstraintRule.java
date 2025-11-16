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
import tech.rollw.common.value.constraint.ValueConstraintRule;
import tech.rollw.common.value.constraint.ValueValidationResult;

/**
 * Constraint rule to check if a number is negative (less than zero).
 *
 * @author RollW
 */
public class NegativeConstraintRule implements ValueConstraintRule<Number> {
    private static final String TYPE = "negative";

    @NonNull
    @Override
    public String getType() {
        return TYPE;
    }

    @NonNull
    @Override
    public ValueValidationResult validate(Number value) {
        if (value == null) {
            return ValueValidationResult.failure("Value cannot be null");
        }

        if (value.doubleValue() >= 0) {
            return ValueValidationResult.failure(
                    "Value must be negative, but was " + value
            );
        }

        return ValueValidationResult.success();
    }

    @NonNull
    @Override
    public Descriptor getDescriptor() {
        return new NegativeDescriptor();
    }

    public static class NegativeDescriptor implements Descriptor {
        @Override
        public String toString() {
            return "Negative";
        }
    }
}

