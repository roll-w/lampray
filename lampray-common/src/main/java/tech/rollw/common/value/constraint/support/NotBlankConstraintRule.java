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
 * Constraint rule to check if a string is not blank (contains at least one non-whitespace character).
 *
 * @author RollW
 */
public class NotBlankConstraintRule implements ValueConstraintRule<String> {
    private static final String TYPE = "not_blank";

    @NonNull
    @Override
    public String getType() {
        return TYPE;
    }

    @NonNull
    @Override
    public ValueValidationResult validate(@Nullable String value) {
        if (value == null) {
            return ValueValidationResult.failure("Value cannot be null");
        }

        if (value.isBlank()) {
            return ValueValidationResult.failure("Value cannot be blank");
        }

        return ValueValidationResult.success();
    }

    @NonNull
    @Override
    public Descriptor getDescriptor() {
        return new NotBlankDescriptor();
    }

    public static class NotBlankDescriptor implements Descriptor {
        @Override
        public String toString() {
            return "NotBlank";
        }
    }
}

