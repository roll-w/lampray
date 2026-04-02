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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author RollW
 */
public final class TagSpecification {
    private final String name;
    private final boolean required;
    private final Set<String> allowedValues;

    private TagSpecification(Builder builder) {
        this.name = SpecificationSupport.requireText(builder.name, "name");
        this.required = builder.required;
        this.allowedValues = Set.copyOf(builder.allowedValues);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public String getName() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

    public Set<String> getAllowedValues() {
        return allowedValues;
    }

    public boolean supports(String value) {
        return allowedValues.isEmpty() || allowedValues.contains(value);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TagSpecification that)) {
            return false;
        }
        return required == that.required
                && Objects.equals(name, that.name)
                && Objects.equals(allowedValues, that.allowedValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, required, allowedValues);
    }

    /**
     * @author RollW
     */
    public static final class Builder {
        private final String name;
        private final Set<String> allowedValues = new LinkedHashSet<>();
        private boolean required;

        private Builder(String name) {
            this.name = SpecificationSupport.requireText(name, "name");
        }

        public Builder required() {
            this.required = true;
            return this;
        }

        public Builder allowedValues(String... allowedValues) {
            Arrays.stream(allowedValues)
                    .map(value -> SpecificationSupport.requireText(value, "allowedValue"))
                    .forEach(this.allowedValues::add);
            return this;
        }

        public TagSpecification build() {
            return new TagSpecification(this);
        }
    }
}
