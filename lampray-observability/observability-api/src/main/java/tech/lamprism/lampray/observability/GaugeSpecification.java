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

import io.micrometer.core.instrument.Gauge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

/**
 * @author RollW
 */
public final class GaugeSpecification<S> extends AbstractObservedMetricSpecification<Gauge, S> {
    private final ToDoubleFunction<S> valueFunction;

    private GaugeSpecification(Builder<S> builder) {
        super(builder.name, builder.tagSpecifications, builder.description, builder.baseUnit, Gauge.class, builder.sourceType);
        this.valueFunction = Objects.requireNonNull(builder.valueFunction, "valueFunction cannot be null");
    }

    public static <S> Builder<S> builder(String name,
                                         Class<S> sourceType,
                                         ToDoubleFunction<S> valueFunction) {
        return new Builder<>(name, sourceType, valueFunction);
    }

    public ToDoubleFunction<S> getValueFunction() {
        return valueFunction;
    }

    /**
     * @author RollW
     */
    public static final class Builder<S> {
        private final String name;
        private final Class<S> sourceType;
        private final ToDoubleFunction<S> valueFunction;
        private final List<TagSpecification> tagSpecifications = new ArrayList<>();
        private String description = "";
        private String baseUnit;

        private Builder(String name,
                        Class<S> sourceType,
                        ToDoubleFunction<S> valueFunction) {
            this.name = SpecificationSupport.requireName(name);
            this.sourceType = Objects.requireNonNull(sourceType, "sourceType cannot be null");
            this.valueFunction = Objects.requireNonNull(valueFunction, "valueFunction cannot be null");
        }

        public Builder<S> description(String description) {
            this.description = SpecificationSupport.normalizeDescription(description);
            return this;
        }

        public Builder<S> baseUnit(String baseUnit) {
            this.baseUnit = SpecificationSupport.normalizeOptionalText(baseUnit, "baseUnit");
            return this;
        }

        public Builder<S> tag(TagSpecification tagSpecification) {
            this.tagSpecifications.add(tagSpecification);
            return this;
        }

        public Builder<S> tags(TagSpecification... tagSpecifications) {
            this.tagSpecifications.addAll(Arrays.asList(tagSpecifications));
            return this;
        }

        public GaugeSpecification<S> build() {
            return new GaugeSpecification<>(this);
        }
    }
}
