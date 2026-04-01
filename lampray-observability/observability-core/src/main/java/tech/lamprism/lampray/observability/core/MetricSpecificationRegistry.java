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
import tech.lamprism.lampray.observability.MetricSpecificationProvider;
import tech.lamprism.lampray.observability.MetricType;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author RollW
 */
final class MetricSpecificationRegistry {
    private final Map<String, MetricSpecification> specifications;

    MetricSpecificationRegistry(List<MetricSpecificationProvider> providers) {
        this.specifications = collectSpecifications(providers);
    }

    MetricSpecification resolve(MetricSpecification specification,
                                MetricType expectedType) {
        Objects.requireNonNull(specification, "specification cannot be null");
        MetricSpecification registeredSpecification = specifications.get(specification.getMetricName());
        if (registeredSpecification == null) {
            throw new IllegalArgumentException("Unregistered metric specification: " + specification.getMetricName());
        }
        if (registeredSpecification.getMetricType() != expectedType) {
            throw new IllegalArgumentException(
                    "Metric type mismatch, expected " + expectedType + " but got " + registeredSpecification.getMetricType()
            );
        }
        return registeredSpecification;
    }

    Map<String, String> normalizeTags(MetricSpecification specification,
                                      Map<String, String> tags) {
        Objects.requireNonNull(tags, "tags cannot be null");
        Map<String, String> normalizedTags = new LinkedHashMap<>();
        tags.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry -> {
                    validateTag(specification, entry.getKey(), entry.getValue());
                    normalizedTags.put(entry.getKey(), entry.getValue());
                });
        return normalizedTags;
    }

    private Map<String, MetricSpecification> collectSpecifications(List<MetricSpecificationProvider> providers) {
        Map<String, MetricSpecification> collectedSpecifications = new LinkedHashMap<>();
        for (MetricSpecificationProvider provider : providers) {
            for (MetricSpecification specification : provider.getMetricSpecifications()) {
                MetricSpecification previous = collectedSpecifications.putIfAbsent(
                        specification.getMetricName(),
                        specification
                );
                if (previous != null) {
                    throw new IllegalArgumentException("Duplicate metric specification: " + specification.getMetricName());
                }
            }
        }
        return Map.copyOf(collectedSpecifications);
    }

    private void validateTag(MetricSpecification specification,
                             String key,
                             String value) {
        if (!specification.getAllowedTags().contains(key)) {
            throw new IllegalArgumentException(
                    "Tag '" + key + "' is not allowed for metric '" + specification.getMetricName() + "'"
            );
        }
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Metric tag values cannot be blank");
        }
    }
}
