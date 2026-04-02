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

import tech.lamprism.lampray.observability.SignalSpecification;
import tech.lamprism.lampray.observability.SignalTag;
import tech.lamprism.lampray.observability.SignalTags;
import tech.lamprism.lampray.observability.TagSpecification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author RollW
 */
final class SpecificationRegistry {
    private final ConcurrentMap<String, SignalSpecification> specifications = new ConcurrentHashMap<>();

    void register(SignalSpecification specification) {
        Objects.requireNonNull(specification, "specification cannot be null");
        SignalSpecification existing = specifications.putIfAbsent(specification.getName(), specification);
        if (existing != null && existing.getClass() != specification.getClass()) {
            throw new IllegalArgumentException("Conflicting specification: " + specification.getName());
        }
    }

    SignalTags normalize(SignalSpecification specification,
                         SignalTags tags) {
        return normalize(specification, tags, true);
    }

    SignalTags normalizePartial(SignalSpecification specification,
                                SignalTags tags) {
        return normalize(specification, tags, false);
    }

    void validateTag(SignalSpecification specification,
                     String key,
                     String value) {
        Objects.requireNonNull(specification, "specification cannot be null");
        TagSpecification tagSpecification = findTagSpecification(specification, key);
        if (tagSpecification == null) {
            throw new IllegalArgumentException(
                    "Tag '" + key + "' is not allowed for signal '" + specification.getName() + "'"
            );
        }
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Signal tag values cannot be blank");
        }
        if (!tagSpecification.supports(value)) {
            throw new IllegalArgumentException(
                    "Tag '" + key + "' does not support value '" + value + "' for signal '" + specification.getName() + "'"
            );
        }
    }

    private SignalTags normalize(SignalSpecification specification,
                                 SignalTags tags,
                                 boolean enforceRequired) {
        Objects.requireNonNull(specification, "specification cannot be null");
        Objects.requireNonNull(tags, "tags cannot be null");

        Map<String, String> provided = new HashMap<>();
        for (SignalTag tag : tags) {
            validateTag(specification, tag.getKey(), tag.getValue());
            provided.put(tag.getKey(), tag.getValue());
        }

        if (enforceRequired) {
            for (TagSpecification tagSpecification : specification.getTagSpecifications()) {
                if (tagSpecification.isRequired() && !provided.containsKey(tagSpecification.getName())) {
                    throw new IllegalArgumentException(
                            "Missing required tag '" + tagSpecification.getName() + "' for signal '" + specification.getName() + "'"
                    );
                }
            }
        }

        ArrayList<SignalTag> normalized = new ArrayList<>(provided.size());
        for (TagSpecification tagSpecification : specification.getTagSpecifications()) {
            String value = provided.get(tagSpecification.getName());
            if (value != null) {
                normalized.add(SignalTag.of(tagSpecification.getName(), value));
            }
        }
        return SignalTags.of(normalized);
    }

    private TagSpecification findTagSpecification(SignalSpecification specification,
                                                  String key) {
        for (TagSpecification tagSpecification : specification.getTagSpecifications()) {
            if (tagSpecification.getName().equals(key)) {
                return tagSpecification;
            }
        }
        return null;
    }
}
