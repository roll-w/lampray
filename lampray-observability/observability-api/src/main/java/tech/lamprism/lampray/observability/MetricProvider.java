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
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

/**
 * @author RollW
 */
public interface MetricProvider {
    Counter counter(MetricSpecification specification,
                    Map<String, String> tags);

    default void increment(MetricSpecification specification,
                           Map<String, String> tags) {
        counter(specification, tags).increment();
    }

    default void increment(MetricSpecification specification,
                           Map<String, String> tags,
                           double amount) {
        counter(specification, tags).increment(amount);
    }

    <T> Gauge gauge(MetricSpecification specification,
                    Map<String, String> tags,
                    T target,
                    ToDoubleFunction<T> valueFunction);

    <T> TimeGauge timeGauge(MetricSpecification specification,
                            Map<String, String> tags,
                            T target,
                            TimeUnit timeUnit,
                            ToDoubleFunction<T> valueFunction);

    DistributionSummary histogram(MetricSpecification specification,
                                  Map<String, String> tags);

    Timer timer(MetricSpecification specification,
                Map<String, String> tags);

    default void recordDuration(MetricSpecification specification,
                                Map<String, String> tags,
                                Duration duration) {
        timer(specification, tags).record(duration);
    }

    LongTaskTimer longTaskTimer(MetricSpecification specification,
                                Map<String, String> tags);

    MetricTask startLongTask(MetricSpecification specification,
                             Map<String, String> tags);

    DistributionSummary distributionSummary(MetricSpecification specification,
                                            Map<String, String> tags);

    default void recordValue(MetricSpecification specification,
                             Map<String, String> tags,
                             double value) {
        switch (specification.getMetricType()) {
            case HISTOGRAM -> histogram(specification, tags).record(value);
            case DISTRIBUTION_SUMMARY -> distributionSummary(specification, tags).record(value);
            default -> throw new IllegalArgumentException(
                    "Metric type does not support value recording: " + specification.getMetricType()
            );
        }
    }

    <T> FunctionCounter functionCounter(MetricSpecification specification,
                                        Map<String, String> tags,
                                        T target,
                                        ToDoubleFunction<T> function);

    <T> FunctionTimer functionTimer(MetricSpecification specification,
                                    Map<String, String> tags,
                                    T target,
                                    ToLongFunction<T> countFunction,
                                    ToDoubleFunction<T> totalTimeFunction,
                                    TimeUnit totalTimeUnit);

    MultiGauge multiGauge(MetricSpecification specification,
                          Map<String, String> tags);
}
