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

import tech.lamprism.lampray.observability.SignalTags;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * @author RollW
 */
final class MeterStore {
    private final ConcurrentMap<MeterCacheKey, RegisteredMeter> meters = new ConcurrentHashMap<>();

    <T> T resolve(String name,
                  SignalTags tags,
                  Class<T> expectedType,
                  Supplier<T> supplier) {
        MeterCacheKey cacheKey = new MeterCacheKey(name, tags);
        RegisteredMeter meter = meters.computeIfAbsent(cacheKey, ignored -> RegisteredMeter.simple(supplier.get()));
        return expectedType.cast(meter.getMeter());
    }

    <T> T resolveObserved(String name,
                          SignalTags tags,
                          Object target,
                          Class<T> expectedType,
                          Supplier<T> supplier) {
        Objects.requireNonNull(target, "target cannot be null");
        MeterCacheKey cacheKey = new MeterCacheKey(name, tags);
        RegisteredMeter meter = meters.compute(cacheKey, (ignored, existing) -> {
            if (existing == null) {
                return RegisteredMeter.observed(target, supplier.get());
            }
            existing.verifyObservedTarget(name, tags, target);
            return existing;
        });
        return expectedType.cast(meter.getMeter());
    }

    /**
     * @author RollW
     */
    private static final class RegisteredMeter {
        private final Object meter;
        private final WeakReference<Object> targetReference;

        private RegisteredMeter(Object meter,
                                WeakReference<Object> targetReference) {
            this.meter = Objects.requireNonNull(meter, "meter cannot be null");
            this.targetReference = targetReference;
        }

        static RegisteredMeter simple(Object meter) {
            return new RegisteredMeter(meter, null);
        }

        static RegisteredMeter observed(Object target,
                                        Object meter) {
            return new RegisteredMeter(meter, new WeakReference<>(target));
        }

        Object getMeter() {
            return meter;
        }

        void verifyObservedTarget(String name,
                                  SignalTags tags,
                                  Object expectedTarget) {
            if (targetReference == null) {
                throw new IllegalStateException("Meter '" + name + "' is not an observed meter");
            }
            Object currentTarget = targetReference.get();
            if (currentTarget == null) {
                throw new IllegalStateException(
                        "Observed meter '" + name + "' with tags " + tags
                                + " lost its target reference. Keep a strong reference to the observed object."
                );
            }
            if (currentTarget != expectedTarget) {
                throw new IllegalArgumentException(
                        "Observed meter '" + name + "' with tags " + tags
                                + " is already bound to a different target instance."
                );
            }
        }
    }
}
