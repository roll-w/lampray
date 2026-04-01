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

import tech.lamprism.lampray.observability.MetricSpecification;
import tech.lamprism.lampray.observability.MetricType;

import java.util.Map;
import java.util.Objects;

/**
 * @author RollW
 */
final class MetricRegistrationRequest {
    private final MetricSpecification specification;
    private final MetricType expectedType;
    private final Map<String, String> tags;
    private final Object target;

    MetricRegistrationRequest(MetricSpecification specification,
                              MetricType expectedType,
                              Map<String, String> tags,
                              Object target) {
        this.specification = Objects.requireNonNull(specification, "specification cannot be null");
        this.expectedType = Objects.requireNonNull(expectedType, "expectedType cannot be null");
        this.tags = Map.copyOf(Objects.requireNonNull(tags, "tags cannot be null"));
        this.target = target;
    }

    MetricSpecification getSpecification() {
        return specification;
    }

    MetricType getExpectedType() {
        return expectedType;
    }

    Map<String, String> getTags() {
        return tags;
    }

    Object getTarget() {
        return target;
    }
}
