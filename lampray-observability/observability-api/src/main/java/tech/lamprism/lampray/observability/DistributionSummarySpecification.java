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

import io.micrometer.core.instrument.DistributionSummary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author RollW
 */
public final class DistributionSummarySpecification extends AbstractMetricSpecification<DistributionSummary> {
    private final boolean histogram;
    private final List<Double> percentiles;
    private final List<Double> serviceLevelObjectives;
    private final Double minimumExpectedValue;
    private final Double maximumExpectedValue;

    private DistributionSummarySpecification(Builder builder) {
        super(builder.name, builder.tagSpecifications, builder.description, builder.baseUnit, DistributionSummary.class);
        this.histogram = builder.histogram;
        this.percentiles = SpecificationSupport.normalizePercentiles(builder.percentiles);
        this.serviceLevelObjectives = SpecificationSupport.normalizePositiveNumbers(
                builder.serviceLevelObjectives,
                "serviceLevelObjectives"
        );
        this.minimumExpectedValue = builder.minimumExpectedValue;
        this.maximumExpectedValue = builder.maximumExpectedValue;
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

    public List<Double> getServiceLevelObjectives() {
        return serviceLevelObjectives;
    }

    public Double getMinimumExpectedValue() {
        return minimumExpectedValue;
    }

    public Double getMaximumExpectedValue() {
        return maximumExpectedValue;
    }

    /**
     * @author RollW
     */
    public static final class Builder {
        private final String name;
        private final List<TagSpecification> tagSpecifications = new ArrayList<>();
        private final List<Double> percentiles = new ArrayList<>();
        private final List<Double> serviceLevelObjectives = new ArrayList<>();
        private String description = "";
        private String baseUnit;
        private boolean histogram;
        private Double minimumExpectedValue;
        private Double maximumExpectedValue;

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

        public Builder histogram() {
            this.histogram = true;
            return this;
        }

        public Builder percentiles(double... percentiles) {
            Arrays.stream(percentiles).forEach(this.percentiles::add);
            return this;
        }

        public Builder serviceLevelObjectives(double... serviceLevelObjectives) {
            Arrays.stream(serviceLevelObjectives).forEach(this.serviceLevelObjectives::add);
            return this;
        }

        public Builder minimumExpectedValue(double minimumExpectedValue) {
            this.minimumExpectedValue = minimumExpectedValue;
            return this;
        }

        public Builder maximumExpectedValue(double maximumExpectedValue) {
            this.maximumExpectedValue = maximumExpectedValue;
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

        public DistributionSummarySpecification build() {
            return new DistributionSummarySpecification(this);
        }
    }
}
