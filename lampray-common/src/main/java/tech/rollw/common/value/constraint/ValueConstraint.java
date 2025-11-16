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

package tech.rollw.common.value.constraint;

import java.util.ArrayList;
import java.util.List;

/**
 * Constraint for config values. Constraints are static declarations
 * that can be exported to frontend for validation.
 *
 * @author RollW
 */
public interface ValueConstraint<V> {
    /**
     * Validate the value against the constraint.
     *
     * @param value the value to validate
     * @return the validation result
     */
    ValueValidationResult validate(V value);

    List<ValueConstraintRule<V>> getConstraintRules();

    /**
     * Get the constraint type.
     *
     * @return the constraint type
     */
    String getType();

    /**
     * Combine this constraint with another constraint using AND logic.
     * Both constraints must pass for the validation to succeed.
     *
     * @param other the other constraint
     * @return a new constraint combining this and other with AND logic
     */
    default ValueConstraint<V> and(ValueConstraint<V> other) {
        return new AndConstraint<>(this, other);
    }

    /**
     * Combine this constraint with another constraint using OR logic.
     * At least one constraint must pass for the validation to succeed.
     *
     * @param other the other constraint
     * @return a new constraint combining this and other with OR logic
     */
    default ValueConstraint<V> or(ValueConstraint<V> other) {
        return new OrConstraint<>(this, other);
    }

    /**
     * Negate this constraint.
     * Validation succeeds only when this constraint fails.
     *
     * @return a new constraint that negates this constraint
     */
    default ValueConstraint<V> not() {
        return new NotConstraint<>(this);
    }

    /**
     * AND constraint combiner.
     */
    class AndConstraint<V> implements ValueConstraint<V> {
        private final ValueConstraint<V> left;
        private final ValueConstraint<V> right;

        public AndConstraint(ValueConstraint<V> left, ValueConstraint<V> right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public ValueValidationResult validate(V value) {
            ValueValidationResult leftResult = left.validate(value);
            if (!leftResult.isValid()) {
                return leftResult;
            }
            return right.validate(value);
        }

        @Override
        public List<ValueConstraintRule<V>> getConstraintRules() {
            List<ValueConstraintRule<V>> rules = new ArrayList<>(left.getConstraintRules());
            rules.addAll(right.getConstraintRules());
            return rules;
        }

        @Override
        public String getType() {
            return "and(" + left.getType() + ", " + right.getType() + ")";
        }
    }

    /**
     * OR constraint combiner.
     */
    class OrConstraint<V> implements ValueConstraint<V> {
        private final ValueConstraint<V> left;
        private final ValueConstraint<V> right;

        public OrConstraint(ValueConstraint<V> left, ValueConstraint<V> right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public ValueValidationResult validate(V value) {
            ValueValidationResult leftResult = left.validate(value);
            if (leftResult.isValid()) {
                return leftResult;
            }
            return right.validate(value);
        }

        @Override
        public List<ValueConstraintRule<V>> getConstraintRules() {
            List<ValueConstraintRule<V>> rules = new ArrayList<>(left.getConstraintRules());
            rules.addAll(right.getConstraintRules());
            return rules;
        }

        @Override
        public String getType() {
            return "or(" + left.getType() + ", " + right.getType() + ")";
        }
    }

    /**
     * NOT constraint combiner.
     */
    class NotConstraint<V> implements ValueConstraint<V> {
        private final ValueConstraint<V> constraint;

        public NotConstraint(ValueConstraint<V> constraint) {
            this.constraint = constraint;
        }

        @Override
        public ValueValidationResult validate(V value) {
            ValueValidationResult result = constraint.validate(value);
            if (result.isValid()) {
                return ValueValidationResult.failure("Value must not satisfy: " + constraint.getType());
            }
            return ValueValidationResult.success();
        }

        @Override
        public List<ValueConstraintRule<V>> getConstraintRules() {
            return constraint.getConstraintRules();
        }

        @Override
        public String getType() {
            return "not(" + constraint.getType() + ")";
        }
    }
}
