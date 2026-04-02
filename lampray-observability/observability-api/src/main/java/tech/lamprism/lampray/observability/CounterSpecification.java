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

import io.micrometer.core.instrument.Counter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author RollW
 */
public final class CounterSpecification extends AbstractMetricSpecification<Counter> {
    private CounterSpecification(Builder builder) {
        super(builder.name, builder.tagSpecifications, builder.description, builder.baseUnit, Counter.class);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * @author RollW
     */
    public static final class Builder {
        private final String name;
        private final List<TagSpecification> tagSpecifications = new ArrayList<>();
        private String description = "";
        private String baseUnit;

        private Builder(String name) {
            this.name = SpecificationSupport.requireName(name);
        }

        public Builder description(String description) {
            this.description = SpecificationSupport.normalizeDescription(description);
            return this;
        }

        public Builder baseUnit(String baseUnit) {
            this.baseUnit = SpecificationSupport.normalizeOptionalText(baseUnit, "baseUnit");
            return this;
        }

        public Builder tag(TagSpecification tagSpecification) {
            this.tagSpecifications.add(tagSpecification);
            return this;
        }

        public Builder tags(TagSpecification... tagSpecifications) {
            this.tagSpecifications.addAll(Arrays.asList(tagSpecifications));
            return this;
        }

        public CounterSpecification build() {
            return new CounterSpecification(this);
        }
    }
}
