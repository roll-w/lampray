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
import tech.lamprism.lampray.observability.ObservationScope;
import tech.lamprism.lampray.observability.ObservationSpecification;

/**
 * @author RollW
 */
final class MicrometerObservationScope implements ObservationScope {
    private final Observation observation;
    private final Observation.Scope scope;
    private final ObservationSpecification specification;
    private final SpecificationRegistry specificationRegistry;
    private boolean closed;

    MicrometerObservationScope(Observation observation,
                               Observation.Scope scope,
                               ObservationSpecification specification,
                               SpecificationRegistry specificationRegistry) {
        this.observation = observation;
        this.scope = scope;
        this.specification = specification;
        this.specificationRegistry = specificationRegistry;
    }

    @Override
    public ObservationScope tag(String key,
                                String value) {
        specificationRegistry.validateTag(specification, key, value);
        observation.lowCardinalityKeyValue(key, value);
        return this;
    }

    @Override
    public void error(Throwable throwable) {
        observation.error(throwable);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        scope.close();
        observation.stop();
    }
}
