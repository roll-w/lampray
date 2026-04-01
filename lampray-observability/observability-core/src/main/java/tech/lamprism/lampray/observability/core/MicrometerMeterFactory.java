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

package tech.lamprism.lampray.observability.core;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import tech.lamprism.lampray.observability.MetricSpecification;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

/**
 * @author RollW
 */
final class MicrometerMeterFactory {
    private final MeterRegistry meterRegistry;

    MicrometerMeterFactory(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    Counter createCounter(MetricSpecification specification,
                          Map<String, String> tags) {
        Counter.Builder builder = Counter.builder(specification.getMetricName())
                .tags(toTagArray(tags));
        applyDescription(builder::description, specification.getMetricDescription());
        applyBaseUnit(builder::baseUnit, specification.getBaseUnit());
        return builder.register(meterRegistry);
    }

    <T> Gauge createGauge(MetricSpecification specification,
                          Map<String, String> tags,
                          T target,
                          ToDoubleFunction<T> valueFunction) {
        Gauge.Builder<T> builder = Gauge.builder(specification.getMetricName(), target, valueFunction)
                .tags(toTagArray(tags));
        applyDescription(builder::description, specification.getMetricDescription());
        applyBaseUnit(builder::baseUnit, specification.getBaseUnit());
        return builder.register(meterRegistry);
    }

    <T> TimeGauge createTimeGauge(MetricSpecification specification,
                                  Map<String, String> tags,
                                  T target,
                                  TimeUnit timeUnit,
                                  ToDoubleFunction<T> valueFunction) {
        TimeGauge.Builder<T> builder = TimeGauge.builder(
                specification.getMetricName(),
                target,
                timeUnit,
                valueFunction
        ).tags(toTagArray(tags));
        applyDescription(builder::description, specification.getMetricDescription());
        return builder.register(meterRegistry);
    }

    DistributionSummary createHistogram(MetricSpecification specification,
                                        Map<String, String> tags) {
        return createSummary(specification, tags, true);
    }

    Timer createTimer(MetricSpecification specification,
                      Map<String, String> tags) {
        Timer.Builder builder = Timer.builder(specification.getMetricName())
                .tags(toTagArray(tags));
        applyDescription(builder::description, specification.getMetricDescription());
        applyTimerConfiguration(builder, specification);
        return builder.register(meterRegistry);
    }

    LongTaskTimer createLongTaskTimer(MetricSpecification specification,
                                      Map<String, String> tags) {
        LongTaskTimer.Builder builder = LongTaskTimer.builder(specification.getMetricName())
                .tags(toTagArray(tags));
        applyDescription(builder::description, specification.getMetricDescription());
        return builder.register(meterRegistry);
    }

    DistributionSummary createDistributionSummary(MetricSpecification specification,
                                                  Map<String, String> tags) {
        return createSummary(specification, tags, specification.isHistogramEnabled());
    }

    <T> FunctionCounter createFunctionCounter(MetricSpecification specification,
                                              Map<String, String> tags,
                                              T target,
                                              ToDoubleFunction<T> function) {
        FunctionCounter.Builder<T> builder = FunctionCounter.builder(
                        specification.getMetricName(),
                        target,
                        function)
                .tags(toTagArray(tags));
        applyDescription(builder::description, specification.getMetricDescription());
        return builder.register(meterRegistry);
    }

    <T> FunctionTimer createFunctionTimer(MetricSpecification specification,
                                          Map<String, String> tags,
                                          T target,
                                          ToLongFunction<T> countFunction,
                                          ToDoubleFunction<T> totalTimeFunction,
                                          TimeUnit totalTimeUnit) {
        FunctionTimer.Builder<T> builder = FunctionTimer.builder(
                        specification.getMetricName(),
                        target,
                        countFunction,
                        totalTimeFunction,
                        totalTimeUnit)
                .tags(toTagArray(tags));
        applyDescription(builder::description, specification.getMetricDescription());
        return builder.register(meterRegistry);
    }

    MultiGauge createMultiGauge(MetricSpecification specification,
                                Map<String, String> tags) {
        MultiGauge.Builder builder = MultiGauge.builder(specification.getMetricName())
                .tags(toTagArray(tags));
        applyDescription(builder::description, specification.getMetricDescription());
        applyBaseUnit(builder::baseUnit, specification.getBaseUnit());
        return builder.register(meterRegistry);
    }

    private DistributionSummary createSummary(MetricSpecification specification,
                                              Map<String, String> tags,
                                              boolean histogramEnabled) {
        DistributionSummary.Builder builder = DistributionSummary.builder(specification.getMetricName())
                .tags(toTagArray(tags));
        applyDescription(builder::description, specification.getMetricDescription());
        applyBaseUnit(builder::baseUnit, specification.getBaseUnit());
        applySummaryConfiguration(builder, specification, histogramEnabled);
        return builder.register(meterRegistry);
    }

    private void applyTimerConfiguration(Timer.Builder builder,
                                         MetricSpecification specification) {
        if (specification.isHistogramEnabled()) {
            builder.publishPercentileHistogram();
        }
        if (specification.getPercentiles().length > 0) {
            builder.publishPercentiles(specification.getPercentiles());
        }
        if (specification.getServiceLevelObjectives().length > 0) {
            builder.serviceLevelObjectives(toDurations(specification.getServiceLevelObjectives()));
        }
    }

    private void applySummaryConfiguration(DistributionSummary.Builder builder,
                                           MetricSpecification specification,
                                           boolean histogramEnabled) {
        if (histogramEnabled) {
            builder.publishPercentileHistogram();
        }
        if (specification.getPercentiles().length > 0) {
            builder.publishPercentiles(specification.getPercentiles());
        }
        if (specification.getServiceLevelObjectives().length > 0) {
            builder.serviceLevelObjectives(specification.getServiceLevelObjectives());
        }
        if (specification.getMinimumExpectedValue() != null) {
            builder.minimumExpectedValue(specification.getMinimumExpectedValue());
        }
        if (specification.getMaximumExpectedValue() != null) {
            builder.maximumExpectedValue(specification.getMaximumExpectedValue());
        }
    }

    private void applyDescription(Consumer<String> consumer,
                                  String description) {
        if (description != null && !description.isBlank()) {
            consumer.accept(description);
        }
    }

    private void applyBaseUnit(Consumer<String> consumer,
                               String baseUnit) {
        if (baseUnit != null && !baseUnit.isBlank()) {
            consumer.accept(baseUnit);
        }
    }

    private String[] toTagArray(Map<String, String> tags) {
        List<String> meterTags = new ArrayList<>();
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            meterTags.add(entry.getKey());
            meterTags.add(entry.getValue());
        }
        return meterTags.toArray(String[]::new);
    }

    private Duration[] toDurations(double[] values) {
        Duration[] durations = new Duration[values.length];
        for (int i = 0; i < values.length; i++) {
            durations[i] = Duration.ofNanos((long) values[i]);
        }
        return durations;
    }
}
