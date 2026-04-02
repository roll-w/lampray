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

import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author RollW
 */
public final class TimerSpecification extends AbstractMetricSpecification<Timer> {
    private final boolean histogram;
    private final List<Double> percentiles;
    private final List<Duration> serviceLevelObjectives;

    private TimerSpecification(Builder builder) {
        super(builder.name, builder.tagSpecifications, builder.description, null, Timer.class);
        this.histogram = builder.histogram;
        this.percentiles = SpecificationSupport.normalizePercentiles(builder.percentiles);
        this.serviceLevelObjectives = SpecificationSupport.normalizeDurations(
                builder.serviceLevelObjectives,
                "serviceLevelObjectives"
        );
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public boolean isHistogram() {
        return histogram;
    }

    public List<Double> getPercentiles() {
        return percentiles;
    }

    public List<Duration> getServiceLevelObjectives() {
        return serviceLevelObjectives;
    }

    /**
     * @author RollW
     */
    public static final class Builder {
        private final String name;
        private final List<TagSpecification> tagSpecifications = new ArrayList<>();
        private final List<Double> percentiles = new ArrayList<>();
        private final List<Duration> serviceLevelObjectives = new ArrayList<>();
        private String description = "";
        private boolean histogram;

        private Builder(String name) {
            this.name = SpecificationSupport.requireName(name);
        }

        public Builder description(String description) {
            this.description = SpecificationSupport.normalizeDescription(description);
            return this;
        }

        public Builder histogram() {
            this.histogram = true;
            return this;
        }

        public Builder percentiles(double... percentiles) {
            Arrays.stream(percentiles).forEach(this.percentiles::add);
            return this;
        }

        public Builder serviceLevelObjectives(Duration... serviceLevelObjectives) {
            this.serviceLevelObjectives.addAll(Arrays.asList(serviceLevelObjectives));
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

        public TimerSpecification build() {
            return new TimerSpecification(this);
        }
    }
}
