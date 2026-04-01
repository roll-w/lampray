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
import tech.lamprism.lampray.observability.ObservationDefinition;
import tech.lamprism.lampray.observability.ObservationScope;
import tech.lamprism.lampray.observability.ObservationTag;
import tech.lamprism.lampray.observability.Observability;

import java.util.Locale;
import java.util.Objects;

/**
 * @author RollW
 */
public class MicrometerObservability implements Observability {
    private final ObservationRegistry observationRegistry;

    public MicrometerObservability(ObservationRegistry observationRegistry) {
        this.observationRegistry = Objects.requireNonNull(observationRegistry,
                "observationRegistry cannot be null");
    }

    @Override
    public ObservationScope openScope(ObservationDefinition definition) {
        Objects.requireNonNull(definition, "definition cannot be null");

        Observation observation = Observation.createNotStarted(definition.getName(), observationRegistry)
                .lowCardinalityKeyValue("domain", definition.getDomain().name().toLowerCase(Locale.ROOT));

        for (ObservationTag tag : definition.getLowCardinalityTags()) {
            observation.lowCardinalityKeyValue(tag.getKey(), tag.getValue());
        }

        observation.start();
        return new MicrometerObservationScope(observation, observation.openScope());
    }
}
