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
import tech.lamprism.lampray.observability.MetricProvider;
import tech.lamprism.lampray.observability.MetricTask;
import tech.lamprism.lampray.observability.MetricSpecification;
import tech.lamprism.lampray.observability.MetricSpecificationProvider;
import tech.lamprism.lampray.observability.MetricType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

/**
 * @author RollW
 */
public class MicrometerMetricProvider implements MetricProvider {
    private final MetricSpecificationRegistry specificationRegistry;
    private final MicrometerMeterFactory meterFactory;
    private final ConcurrentMap<MetricCacheKey, Object> metrics = new ConcurrentHashMap<>();

    public MicrometerMetricProvider(List<MetricSpecificationProvider> specificationProviders,
                                    MeterRegistry meterRegistry) {
        this.specificationRegistry = new MetricSpecificationRegistry(specificationProviders);
        this.meterFactory = new MicrometerMeterFactory(Objects.requireNonNull(meterRegistry,
                "meterRegistry cannot be null"));
    }

    @Override
    public Counter counter(MetricSpecification specification,
                           Map<String, String> tags) {
        MetricRegistrationRequest request = request(specification, MetricType.COUNTER, tags, null);
        return resolve(request, Counter.class,
                () -> meterFactory.createCounter(request.getSpecification(), request.getTags()));
    }

    @Override
    public <T> Gauge gauge(MetricSpecification specification,
                           Map<String, String> tags,
                           T target,
                           ToDoubleFunction<T> valueFunction) {
        Objects.requireNonNull(target, "target cannot be null");
        Objects.requireNonNull(valueFunction, "valueFunction cannot be null");
        MetricRegistrationRequest request = request(specification, MetricType.GAUGE, tags, target);
        return resolve(request, Gauge.class,
                () -> meterFactory.createGauge(request.getSpecification(), request.getTags(), target, valueFunction));
    }

    @Override
    public <T> TimeGauge timeGauge(MetricSpecification specification,
                                   Map<String, String> tags,
                                   T target,
                                   TimeUnit timeUnit,
                                   ToDoubleFunction<T> valueFunction) {
        Objects.requireNonNull(target, "target cannot be null");
        Objects.requireNonNull(timeUnit, "timeUnit cannot be null");
        Objects.requireNonNull(valueFunction, "valueFunction cannot be null");
        MetricRegistrationRequest request = request(specification, MetricType.TIME_GAUGE, tags, target);
        return resolve(request, TimeGauge.class,
                () -> meterFactory.createTimeGauge(request.getSpecification(), request.getTags(), target, timeUnit, valueFunction));
    }

    @Override
    public DistributionSummary histogram(MetricSpecification specification,
                                         Map<String, String> tags) {
        MetricRegistrationRequest request = request(specification, MetricType.HISTOGRAM, tags, null);
        return resolve(request, DistributionSummary.class,
                () -> meterFactory.createHistogram(request.getSpecification(), request.getTags()));
    }

    @Override
    public Timer timer(MetricSpecification specification,
                       Map<String, String> tags) {
        MetricRegistrationRequest request = request(specification, MetricType.TIMER, tags, null);
        return resolve(request, Timer.class,
                () -> meterFactory.createTimer(request.getSpecification(), request.getTags()));
    }

    @Override
    public LongTaskTimer longTaskTimer(MetricSpecification specification,
                                       Map<String, String> tags) {
        MetricRegistrationRequest request = request(specification, MetricType.LONG_TASK_TIMER, tags, null);
        return resolve(request, LongTaskTimer.class,
                () -> meterFactory.createLongTaskTimer(request.getSpecification(), request.getTags()));
    }

    @Override
    public MetricTask startLongTask(MetricSpecification specification,
                                    Map<String, String> tags) {
        LongTaskTimer.Sample sample = longTaskTimer(specification, tags).start();
        return sample::stop;
    }

    @Override
    public DistributionSummary distributionSummary(MetricSpecification specification,
                                                   Map<String, String> tags) {
        MetricRegistrationRequest request = request(specification, MetricType.DISTRIBUTION_SUMMARY, tags, null);
        return resolve(request, DistributionSummary.class,
                () -> meterFactory.createDistributionSummary(request.getSpecification(), request.getTags()));
    }

    @Override
    public <T> FunctionCounter functionCounter(MetricSpecification specification,
                                               Map<String, String> tags,
                                               T target,
                                               ToDoubleFunction<T> function) {
        Objects.requireNonNull(target, "target cannot be null");
        Objects.requireNonNull(function, "function cannot be null");
        MetricRegistrationRequest request = request(specification, MetricType.FUNCTION_COUNTER, tags, target);
        return resolve(request, FunctionCounter.class,
                () -> meterFactory.createFunctionCounter(
                        request.getSpecification(),
                        request.getTags(),
                        target,
                        function));
    }

    @Override
    public <T> FunctionTimer functionTimer(MetricSpecification specification,
                                           Map<String, String> tags,
                                           T target,
                                           ToLongFunction<T> countFunction,
                                           ToDoubleFunction<T> totalTimeFunction,
                                           TimeUnit totalTimeUnit) {
        Objects.requireNonNull(target, "target cannot be null");
        Objects.requireNonNull(countFunction, "countFunction cannot be null");
        Objects.requireNonNull(totalTimeFunction, "totalTimeFunction cannot be null");
        Objects.requireNonNull(totalTimeUnit, "totalTimeUnit cannot be null");
        MetricRegistrationRequest request = request(specification, MetricType.FUNCTION_TIMER, tags, target);
        return resolve(request, FunctionTimer.class,
                () -> meterFactory.createFunctionTimer(
                        request.getSpecification(),
                        request.getTags(),
                        target,
                        countFunction,
                        totalTimeFunction,
                        totalTimeUnit));
    }

    @Override
    public MultiGauge multiGauge(MetricSpecification specification,
                                 Map<String, String> tags) {
        MetricRegistrationRequest request = request(specification, MetricType.MULTI_GAUGE, tags, null);
        return resolve(request, MultiGauge.class,
                () -> meterFactory.createMultiGauge(request.getSpecification(), request.getTags()));
    }

    private MetricRegistrationRequest request(MetricSpecification specification,
                                              MetricType expectedType,
                                              Map<String, String> tags,
                                              Object target) {
        MetricSpecification registeredSpecification = specificationRegistry.resolve(specification, expectedType);
        Map<String, String> normalizedTags = specificationRegistry.normalizeTags(registeredSpecification, tags);
        return new MetricRegistrationRequest(registeredSpecification, expectedType, normalizedTags, target);
    }

    private <T> T resolve(MetricRegistrationRequest request,
                          Class<T> expectedType,
                          Supplier<T> supplier) {
        MetricCacheKey cacheKey = new MetricCacheKey(
                request.getSpecification().getMetricName(),
                request.getExpectedType(),
                request.getTags(),
                request.getTarget() == null ? null : request.getTarget().getClass().getName()
        );
        Object metric = metrics.computeIfAbsent(cacheKey, ignored -> supplier.get());
        return expectedType.cast(metric);
    }
}
