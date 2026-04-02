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

package tech.lamprism.lampray.web.observability.management;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author RollW
 */
public final class ManagementRequest {
    private final List<String> selectors;
    private final Map<String, List<String>> queryParameters;

    public ManagementRequest(List<String> selectors,
                             Map<String, List<String>> queryParameters) {
        Objects.requireNonNull(selectors, "selectors cannot be null");
        Objects.requireNonNull(queryParameters, "queryParameters cannot be null");
        this.selectors = List.copyOf(selectors);

        LinkedHashMap<String, List<String>> copied = new LinkedHashMap<>();
        queryParameters.forEach((key, value) -> copied.put(key, List.copyOf(value)));
        this.queryParameters = Collections.unmodifiableMap(copied);
    }

    public static ManagementRequest empty() {
        return new ManagementRequest(List.of(), Map.of());
    }

    public List<String> getSelectors() {
        return selectors;
    }

    public int getSelectorCount() {
        return selectors.size();
    }

    public String getSelector(int index) {
        return selectors.get(index);
    }

    public Map<String, List<String>> getQueryParameters() {
        return queryParameters;
    }

    public List<String> getQueryParameter(String name) {
        List<String> values = queryParameters.get(name);
        return values == null ? List.of() : values;
    }
}
