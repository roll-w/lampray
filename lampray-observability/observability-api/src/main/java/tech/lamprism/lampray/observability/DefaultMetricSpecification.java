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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
public final class DefaultMetricSpecification implements MetricSpecification {
    private final List<String> allowedTags;
    private final String metricName;
    private final String metricDescription;
    private final MetricType metricType;
    private final String baseUnit;
    private final boolean histogramEnabled;
    private final double[] percentiles;
    private final double[] serviceLevelObjectives;
    private final Double minimumExpectedValue;
    private final Double maximumExpectedValue;

    private DefaultMetricSpecification(Builder builder) {
        this.allowedTags = List.copyOf(builder.allowedTags);
        this.metricName = builder.metricName;
        this.metricDescription = builder.metricDescription;
        this.metricType = builder.metricType;
        this.baseUnit = builder.baseUnit;
        this.histogramEnabled = builder.histogramEnabled;
        this.percentiles = builder.percentiles.clone();
        this.serviceLevelObjectives = builder.serviceLevelObjectives.clone();
        this.minimumExpectedValue = builder.minimumExpectedValue;
        this.maximumExpectedValue = builder.maximumExpectedValue;
    }

    public static Builder builder(String metricName,
                                  MetricType metricType) {
        return new Builder(metricName, metricType);
    }

    @Override
    public List<String> getAllowedTags() {
        return allowedTags;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }

    @Override
    public String getMetricDescription() {
        return metricDescription;
    }

    @Override
    public MetricType getMetricType() {
        return metricType;
    }

    @Override
    public String getBaseUnit() {
        return baseUnit;
    }

    @Override
    public boolean isHistogramEnabled() {
        return histogramEnabled;
    }

    @Override
    public double[] getPercentiles() {
        return percentiles.clone();
    }

    @Override
    public double[] getServiceLevelObjectives() {
        return serviceLevelObjectives.clone();
    }

    @Override
    public Double getMinimumExpectedValue() {
        return minimumExpectedValue;
    }

    @Override
    public Double getMaximumExpectedValue() {
        return maximumExpectedValue;
    }

    /**
     * @author RollW
     */
    public static final class Builder {
        private final List<String> allowedTags = new ArrayList<>();
        private final String metricName;
        private final MetricType metricType;
        private String metricDescription = "";
        private String baseUnit;
        private boolean histogramEnabled;
        private double[] percentiles = new double[0];
        private double[] serviceLevelObjectives = new double[0];
        private Double minimumExpectedValue;
        private Double maximumExpectedValue;

        private Builder(String metricName,
                        MetricType metricType) {
            this.metricName = requireText(metricName, "metricName");
            this.metricType = Objects.requireNonNull(metricType, "metricType cannot be null");
        }

        public Builder description(String metricDescription) {
            this.metricDescription = requireText(metricDescription, "metricDescription");
            return this;
        }

        public Builder allowTags(String... tags) {
            Objects.requireNonNull(tags, "tags cannot be null");
            for (String tag : tags) {
                allowedTags.add(requireText(tag, "tag"));
            }
            return this;
        }

        public Builder baseUnit(String baseUnit) {
            this.baseUnit = requireText(baseUnit, "baseUnit");
            return this;
        }

        public Builder histogram() {
            this.histogramEnabled = true;
            return this;
        }

        public Builder percentiles(double... percentiles) {
            Objects.requireNonNull(percentiles, "percentiles cannot be null");
            this.percentiles = percentiles.clone();
            return this;
        }

        public Builder serviceLevelObjectives(double... serviceLevelObjectives) {
            Objects.requireNonNull(serviceLevelObjectives, "serviceLevelObjectives cannot be null");
            this.serviceLevelObjectives = serviceLevelObjectives.clone();
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

        public DefaultMetricSpecification build() {
            return new DefaultMetricSpecification(this);
        }

        private static String requireText(String value,
                                          String fieldName) {
            Objects.requireNonNull(value, fieldName + " cannot be null");
            if (value.trim().isEmpty()) {
                throw new IllegalArgumentException(fieldName + " cannot be blank");
            }
            return value;
        }
    }
}
