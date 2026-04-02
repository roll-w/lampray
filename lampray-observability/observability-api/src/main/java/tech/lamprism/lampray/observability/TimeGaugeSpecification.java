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

import io.micrometer.core.instrument.TimeGauge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;

/**
 * @author RollW
 */
public final class TimeGaugeSpecification<S> extends AbstractObservedMetricSpecification<TimeGauge, S> {
    private final TimeUnit timeUnit;
    private final ToDoubleFunction<S> valueFunction;

    private TimeGaugeSpecification(Builder<S> builder) {
        super(builder.name, builder.tagSpecifications, builder.description, null, TimeGauge.class, builder.sourceType);
        this.timeUnit = Objects.requireNonNull(builder.timeUnit, "timeUnit cannot be null");
        this.valueFunction = Objects.requireNonNull(builder.valueFunction, "valueFunction cannot be null");
    }

    public static <S> Builder<S> builder(String name,
                                         Class<S> sourceType,
                                         TimeUnit timeUnit,
                                         ToDoubleFunction<S> valueFunction) {
        return new Builder<>(name, sourceType, timeUnit, valueFunction);
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
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
        private final TimeUnit timeUnit;
        private final ToDoubleFunction<S> valueFunction;
        private final List<TagSpecification> tagSpecifications = new ArrayList<>();
        private String description = "";

        private Builder(String name,
                        Class<S> sourceType,
                        TimeUnit timeUnit,
                        ToDoubleFunction<S> valueFunction) {
            this.name = SpecificationSupport.requireName(name);
            this.sourceType = Objects.requireNonNull(sourceType, "sourceType cannot be null");
            this.timeUnit = Objects.requireNonNull(timeUnit, "timeUnit cannot be null");
            this.valueFunction = Objects.requireNonNull(valueFunction, "valueFunction cannot be null");
        }

        public Builder<S> description(String description) {
            this.description = SpecificationSupport.normalizeDescription(description);
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

        public TimeGaugeSpecification<S> build() {
            return new TimeGaugeSpecification<>(this);
        }
    }
}
