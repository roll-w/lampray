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

/**
 * @author RollW
 */
final class MicrometerObservationScope implements ObservationScope {
    private final Observation observation;
    private final Observation.Scope scope;
    private boolean closed;

    MicrometerObservationScope(Observation observation, Observation.Scope scope) {
        this.observation = observation;
        this.scope = scope;
    }

    @Override
    public void lowCardinalityTag(String key, String value) {
        observation.lowCardinalityKeyValue(key, value);
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
