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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import tech.lamprism.lampray.observability.ObservationScope;
import tech.lamprism.lampray.observability.ObservationSpecification;
import tech.lamprism.lampray.observability.Observations;
import tech.lamprism.lampray.observability.SignalTag;
import tech.lamprism.lampray.observability.SignalTags;

import java.util.Locale;
import java.util.Objects;

/**
 * @author RollW
 */
public final class MicrometerObservations implements Observations {
    private final ObservationRegistry observationRegistry;
    private final SpecificationRegistry specificationRegistry = new SpecificationRegistry();

    public MicrometerObservations(ObservationRegistry observationRegistry) {
        this.observationRegistry = Objects.requireNonNull(observationRegistry,
                "observationRegistry cannot be null");
    }

    @Override
    public ObservationScope open(ObservationSpecification specification,
                                 SignalTags tags) {
        Objects.requireNonNull(specification, "specification cannot be null");
        specificationRegistry.register(specification);
        SignalTags normalizedTags = specificationRegistry.normalizePartial(specification, tags);

        Observation observation = Observation.createNotStarted(specification.getName(), observationRegistry)
                .lowCardinalityKeyValue("domain", specification.getDomain().name().toLowerCase(Locale.ROOT));
        for (SignalTag tag : normalizedTags) {
            observation.lowCardinalityKeyValue(tag.getKey(), tag.getValue());
        }

        observation.start();
        return new MicrometerObservationScope(observation, observation.openScope(), specification, specificationRegistry);
    }
}
