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

import org.springframework.stereotype.Component;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.CommonRuntimeException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class ManagementEndpointRegistry {
    private final Map<String, ManagementEndpoint> endpoints = new LinkedHashMap<>();
    private final ManagementExposurePolicy exposurePolicy;

    public ManagementEndpointRegistry(List<ManagementEndpoint> endpoints,
                                      ManagementExposurePolicy exposurePolicy) {
        Objects.requireNonNull(endpoints, "endpoints cannot be null");
        this.exposurePolicy = exposurePolicy;
        for (ManagementEndpoint endpoint : endpoints) {
            String id = normalize(endpoint.getDescriptor().getId());
            ManagementEndpoint existing = this.endpoints.putIfAbsent(id, endpoint);
            if (existing != null) {
                throw new IllegalArgumentException("Duplicate management endpoint: " + id);
            }
        }
    }

    public boolean isExposed(String endpointId) {
        return endpoint(endpointId) != null && exposurePolicy.isExposed(endpointId);
    }

    public ManagementEndpoint resolve(String endpointId) {
        ManagementEndpoint endpoint = endpoint(endpointId);
        if (endpoint == null || !exposurePolicy.isExposed(endpointId)) {
            throw new CommonRuntimeException(CommonErrorCode.ERROR_NOT_FOUND);
        }
        return endpoint;
    }

    public List<ManagementEndpointDescriptor> exposedDescriptors() {
        ArrayList<ManagementEndpointDescriptor> descriptors = new ArrayList<>();
        for (ManagementEndpoint endpoint : endpoints.values()) {
            if (exposurePolicy.isExposed(endpoint.getDescriptor().getId())) {
                descriptors.add(endpoint.getDescriptor());
            }
        }
        return descriptors;
    }

    public String[] aliasPaths() {
        ArrayList<String> aliases = new ArrayList<>();
        for (ManagementEndpoint endpoint : endpoints.values()) {
            ManagementEndpointDescriptor descriptor = endpoint.getDescriptor();
            if (descriptor.getAliasPath() != null) {
                aliases.add(descriptor.getAliasPath());
            }
        }
        return aliases.toArray(String[]::new);
    }

    public ManagementEndpointDescriptor resolveAlias(String aliasPath) {
        for (ManagementEndpoint endpoint : endpoints.values()) {
            ManagementEndpointDescriptor descriptor = endpoint.getDescriptor();
            if (descriptor.getAliasPath() == null) {
                continue;
            }
            if (descriptor.getAliasPath().equals(aliasPath) && exposurePolicy.isExposed(descriptor.getId())) {
                return descriptor;
            }
        }
        return null;
    }

    private ManagementEndpoint endpoint(String endpointId) {
        return endpoints.get(normalize(endpointId));
    }

    private String normalize(String value) {
        Objects.requireNonNull(value, "value cannot be null");
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
