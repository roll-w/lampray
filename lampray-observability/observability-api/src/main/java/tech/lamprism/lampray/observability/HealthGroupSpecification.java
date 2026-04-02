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

import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
public final class HealthGroupSpecification {
    private final String name;
    private final List<String> contributorNames;

    public HealthGroupSpecification(String name,
                                    List<String> contributorNames) {
        this.name = normalize(name);
        this.contributorNames = List.copyOf(Objects.requireNonNull(contributorNames, "contributorNames cannot be null"));
    }

    public String getName() {
        return name;
    }

    public List<String> getContributorNames() {
        return contributorNames;
    }

    private String normalize(String value) {
        Objects.requireNonNull(value, "name cannot be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("health group name cannot be blank");
        }
        return normalized;
    }
}
