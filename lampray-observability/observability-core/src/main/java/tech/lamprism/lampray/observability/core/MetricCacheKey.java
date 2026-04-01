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

import tech.lamprism.lampray.observability.MetricType;

import java.util.Map;
import java.util.Objects;

/**
 * @author RollW
 */
final class MetricCacheKey {
    private final String name;
    private final MetricType type;
    private final Map<String, String> tags;
    private final String targetType;

    MetricCacheKey(String name,
                   MetricType type,
                   Map<String, String> tags,
                   String targetType) {
        this.name = name;
        this.type = type;
        this.tags = Map.copyOf(tags);
        this.targetType = targetType;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof MetricCacheKey that)) {
            return false;
        }
        return Objects.equals(name, that.name)
                && type == that.type
                && Objects.equals(tags, that.tags)
                && Objects.equals(targetType, that.targetType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, tags, targetType);
    }
}
