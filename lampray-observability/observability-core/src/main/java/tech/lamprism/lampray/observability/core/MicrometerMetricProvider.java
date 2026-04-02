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

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import tech.lamprism.lampray.observability.MetricProvider;
import tech.lamprism.lampray.observability.MetricSpecification;
import tech.lamprism.lampray.observability.ObservedMetricSpecification;
import tech.lamprism.lampray.observability.SignalTags;

import java.util.Objects;

/**
 * @author RollW
 */
public final class MicrometerMetricProvider implements MetricProvider {
    private final SpecificationRegistry specificationRegistry = new SpecificationRegistry();
    private final MeterStore meterStore = new MeterStore();
    private final MicrometerMeterFactory meterFactory;

    public MicrometerMetricProvider(MeterRegistry meterRegistry) {
        this.meterFactory = new MicrometerMeterFactory(Objects.requireNonNull(meterRegistry,
                "meterRegistry cannot be null"));
    }

    @Override
    public <T extends Meter> T meter(MetricSpecification<T> specification,
                                     SignalTags tags) {
        Objects.requireNonNull(specification, "specification cannot be null");
        specificationRegistry.register(specification);
        SignalTags normalized = specificationRegistry.normalize(specification, tags);
        return meterStore.resolve(
                specification.getName(),
                normalized,
                specification.getMeterType(),
                () -> meterFactory.create(specification, normalized)
        );
    }

    @Override
    public <T extends Meter, S> T meter(ObservedMetricSpecification<T, S> specification,
                                        SignalTags tags,
                                        S source) {
        Objects.requireNonNull(specification, "specification cannot be null");
        Objects.requireNonNull(source, "source cannot be null");
        specificationRegistry.register(specification);
        SignalTags normalized = specificationRegistry.normalize(specification, tags);
        return meterStore.resolveObserved(
                specification.getName(),
                normalized,
                source,
                specification.getMeterType(),
                () -> meterFactory.createObserved(specification, normalized, source)
        );
    }
}
