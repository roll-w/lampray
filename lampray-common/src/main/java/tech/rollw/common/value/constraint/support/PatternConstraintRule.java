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
 * Constraint rule to check if a string matches a pattern.
 * Note: This implementation does not use regular expressions as per requirements.
 * It only supports simple pattern matching like prefix, suffix, contains, etc.
 *
 * @author RollW
 */
public class PatternConstraintRule implements ValueConstraintRule<String> {
    private static final String TYPE = "pattern";

    private final String pattern;
    private final PatternType patternType;

    public PatternConstraintRule(String pattern, PatternType patternType) {
        this.pattern = pattern;
        this.patternType = patternType;
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

        boolean matches = switch (patternType) {
            case PREFIX -> value.startsWith(pattern);
            case SUFFIX -> value.endsWith(pattern);
            case CONTAINS -> value.contains(pattern);
            case EQUALS -> value.equals(pattern);
            case EQUALS_IGNORE_CASE -> value.equalsIgnoreCase(pattern);
        };

        if (!matches) {
            return ValueValidationResult.failure(
                    "Value does not match pattern: " + patternType.name() + " '" + pattern + "'"
            );
        }

        return ValueValidationResult.success();
    }

    @Override
    public Descriptor getDescriptor() {
        return new PatternDescriptor(pattern, patternType);
    }

    public enum PatternType {
        PREFIX,
        SUFFIX,
        CONTAINS,
        EQUALS,
        EQUALS_IGNORE_CASE
    }

    public static class PatternDescriptor implements Descriptor {
        private final String pattern;
        private final PatternType patternType;

        public PatternDescriptor(String pattern, PatternType patternType) {
            this.pattern = pattern;
            this.patternType = patternType;
        }

        public String getPattern() {
            return pattern;
        }

        public PatternType getPatternType() {
            return patternType;
        }

        @Override
        public String toString() {
            return "Pattern{type=" + patternType + ", pattern='" + pattern + "'}";
        }
    }
}

