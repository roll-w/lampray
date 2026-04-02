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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author RollW
 */
abstract class AbstractSignalSpecification implements SignalSpecification {
    private final String name;
    private final List<TagSpecification> tagSpecifications;

    protected AbstractSignalSpecification(String name,
                                          List<TagSpecification> tagSpecifications) {
        this.name = SpecificationSupport.requireName(name);
        this.tagSpecifications = List.copyOf(tagSpecifications);
        validateUniqueTagNames(this.tagSpecifications);
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final List<TagSpecification> getTagSpecifications() {
        return tagSpecifications;
    }

    private static void validateUniqueTagNames(List<TagSpecification> tagSpecifications) {
        Set<String> names = new HashSet<>();
        for (TagSpecification tagSpecification : tagSpecifications) {
            if (!names.add(tagSpecification.getName())) {
                throw new IllegalArgumentException("Duplicate tag specification: " + tagSpecification.getName());
            }
        }
    }
}
