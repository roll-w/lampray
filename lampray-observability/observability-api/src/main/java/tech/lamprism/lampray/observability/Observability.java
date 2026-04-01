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

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Provides an implementation-neutral entry point for observability.
 *
 * @author RollW
 */
public interface Observability {
    ObservationScope openScope(ObservationDefinition definition);

    default void observe(ObservationDefinition definition, Consumer<ObservationScope> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        ObservationScope scope = openScope(definition);
        try {
            consumer.accept(scope);
        } catch (RuntimeException | Error ex) {
            scope.error(ex);
            throw ex;
        } finally {
            scope.close();
        }
    }

    default <T> T observe(ObservationDefinition definition, Function<ObservationScope, T> function) {
        Objects.requireNonNull(function, "function cannot be null");
        ObservationScope scope = openScope(definition);
        try {
            return function.apply(scope);
        } catch (RuntimeException | Error ex) {
            scope.error(ex);
            throw ex;
        } finally {
            scope.close();
        }
    }
}
