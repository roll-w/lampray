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
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import tech.lamprism.lampray.observability.CounterSpecification;
import tech.lamprism.lampray.observability.DistributionSummarySpecification;
import tech.lamprism.lampray.observability.GaugeSpecification;
import tech.lamprism.lampray.observability.LongTaskTimerSpecification;
import tech.lamprism.lampray.observability.MetricSpecification;
import tech.lamprism.lampray.observability.ObservedMetricSpecification;
import tech.lamprism.lampray.observability.SignalTag;
import tech.lamprism.lampray.observability.SignalTags;
import tech.lamprism.lampray.observability.TimeGaugeSpecification;
import tech.lamprism.lampray.observability.TimerSpecification;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author RollW
 */
final class MicrometerMeterFactory {
    private final MeterRegistry meterRegistry;

    MicrometerMeterFactory(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    <T extends Meter> T create(MetricSpecification<T> specification,
                               SignalTags tags) {
        if (specification instanceof CounterSpecification counterSpecification) {
            return specification.getMeterType().cast(createCounter(counterSpecification, tags));
        }
        if (specification instanceof TimerSpecification timerSpecification) {
            return specification.getMeterType().cast(createTimer(timerSpecification, tags));
        }
        if (specification instanceof DistributionSummarySpecification summarySpecification) {
            return specification.getMeterType().cast(createDistributionSummary(summarySpecification, tags));
        }
        if (specification instanceof LongTaskTimerSpecification longTaskTimerSpecification) {
            return specification.getMeterType().cast(createLongTaskTimer(longTaskTimerSpecification, tags));
        }
        throw new IllegalArgumentException("Unsupported metric specification: " + specification.getClass().getName());
    }

    <T extends Meter, S> T createObserved(ObservedMetricSpecification<T, S> specification,
                                          SignalTags tags,
                                          S source) {
        if (specification instanceof GaugeSpecification<?> gaugeSpecification) {
            return specification.getMeterType().cast(createGauge(gaugeSpecification, tags, source));
        }
        if (specification instanceof TimeGaugeSpecification<?> timeGaugeSpecification) {
            return specification.getMeterType().cast(createTimeGauge(timeGaugeSpecification, tags, source));
        }
        throw new IllegalArgumentException("Unsupported observed metric specification: " + specification.getClass().getName());
    }

    private Counter createCounter(CounterSpecification specification,
                                  SignalTags tags) {
        Counter.Builder builder = Counter.builder(specification.getName()).tags(toTagArray(tags));
        applyDescription(builder::description, specification.getDescription());
        applyBaseUnit(builder::baseUnit, specification.getBaseUnit());
        return builder.register(meterRegistry);
    }

    private Timer createTimer(TimerSpecification specification,
                              SignalTags tags) {
        Timer.Builder builder = Timer.builder(specification.getName()).tags(toTagArray(tags));
        applyDescription(builder::description, specification.getDescription());
        if (specification.isHistogram()) {
            builder.publishPercentileHistogram();
        }
        if (!specification.getPercentiles().isEmpty()) {
            builder.publishPercentiles(specification.getPercentiles().stream().mapToDouble(Double::doubleValue).toArray());
        }
        if (!specification.getServiceLevelObjectives().isEmpty()) {
            builder.serviceLevelObjectives(specification.getServiceLevelObjectives().toArray(Duration[]::new));
        }
        return builder.register(meterRegistry);
    }

    private DistributionSummary createDistributionSummary(DistributionSummarySpecification specification,
                                                          SignalTags tags) {
        DistributionSummary.Builder builder = DistributionSummary.builder(specification.getName()).tags(toTagArray(tags));
        applyDescription(builder::description, specification.getDescription());
        applyBaseUnit(builder::baseUnit, specification.getBaseUnit());
        if (specification.isHistogram()) {
            builder.publishPercentileHistogram();
        }
        if (!specification.getPercentiles().isEmpty()) {
            builder.publishPercentiles(specification.getPercentiles().stream().mapToDouble(Double::doubleValue).toArray());
        }
        if (!specification.getServiceLevelObjectives().isEmpty()) {
            builder.serviceLevelObjectives(specification.getServiceLevelObjectives().stream().mapToDouble(Double::doubleValue).toArray());
        }
        if (specification.getMinimumExpectedValue() != null) {
            builder.minimumExpectedValue(specification.getMinimumExpectedValue());
        }
        if (specification.getMaximumExpectedValue() != null) {
            builder.maximumExpectedValue(specification.getMaximumExpectedValue());
        }
        return builder.register(meterRegistry);
    }

    private LongTaskTimer createLongTaskTimer(LongTaskTimerSpecification specification,
                                              SignalTags tags) {
        LongTaskTimer.Builder builder = LongTaskTimer.builder(specification.getName()).tags(toTagArray(tags));
        applyDescription(builder::description, specification.getDescription());
        return builder.register(meterRegistry);
    }

    private Gauge createGauge(GaugeSpecification<?> specification,
                              SignalTags tags,
                              Object source) {
        return createGaugeTyped(specification, tags, source);
    }

    private <S> Gauge createGaugeTyped(GaugeSpecification<S> specification,
                                       SignalTags tags,
                                       Object source) {
        S typedSource = specification.getSourceType().cast(source);
        Gauge.Builder<S> builder = Gauge.builder(specification.getName(), typedSource, specification.getValueFunction())
                .tags(toTagArray(tags));
        applyDescription(builder::description, specification.getDescription());
        applyBaseUnit(builder::baseUnit, specification.getBaseUnit());
        return builder.register(meterRegistry);
    }

    private TimeGauge createTimeGauge(TimeGaugeSpecification<?> specification,
                                      SignalTags tags,
                                      Object source) {
        return createTimeGaugeTyped(specification, tags, source);
    }

    private <S> TimeGauge createTimeGaugeTyped(TimeGaugeSpecification<S> specification,
                                               SignalTags tags,
                                               Object source) {
        S typedSource = specification.getSourceType().cast(source);
        TimeGauge.Builder<S> builder = TimeGauge.builder(
                        specification.getName(),
                        typedSource,
                        specification.getTimeUnit(),
                        specification.getValueFunction())
                .tags(toTagArray(tags));
        applyDescription(builder::description, specification.getDescription());
        return builder.register(meterRegistry);
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

    private String[] toTagArray(SignalTags tags) {
        List<String> values = new ArrayList<>();
        for (SignalTag tag : tags) {
            values.add(tag.getKey());
            values.add(tag.getValue());
        }
        return values.toArray(String[]::new);
    }
}
