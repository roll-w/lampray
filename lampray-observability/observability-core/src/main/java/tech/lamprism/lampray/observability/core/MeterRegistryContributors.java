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

import io.micrometer.core.instrument.MeterRegistry;
import tech.lamprism.lampray.observability.MeterRegistryContributor;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
public final class MeterRegistryContributors {
    private final List<MeterRegistryContributor> contributors;

    public MeterRegistryContributors(List<MeterRegistryContributor> contributors) {
        Objects.requireNonNull(contributors, "contributors cannot be null");
        this.contributors = contributors.stream()
                .sorted(Comparator.comparingInt(MeterRegistryContributor::getOrder))
                .toList();
    }

    public void contribute(MeterRegistry meterRegistry) {
        Objects.requireNonNull(meterRegistry, "meterRegistry cannot be null");
        for (MeterRegistryContributor contributor : contributors) {
            contributor.contribute(meterRegistry);
        }
    }
}
