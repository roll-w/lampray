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

package tech.lamprism.lampray.web.controller.observability;

import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import tech.lamprism.lampray.web.controller.AdminApi;
import tech.lamprism.lampray.web.observability.management.ManagementEndpoint;
import tech.lamprism.lampray.web.observability.management.ManagementEndpointDescriptor;
import tech.lamprism.lampray.web.observability.management.ManagementEndpointRegistry;
import tech.lamprism.lampray.web.observability.management.ManagementPaths;
import tech.lamprism.lampray.web.observability.management.ManagementRequest;
import tech.lamprism.lampray.web.observability.management.ManagementResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author RollW
 */
@AdminApi
public class ObservabilityManagementController {
    private final ManagementEndpointRegistry endpointRegistry;

    public ObservabilityManagementController(ManagementEndpointRegistry endpointRegistry) {
        this.endpointRegistry = endpointRegistry;
    }

    @GetMapping(ManagementPaths.ADMIN_ROOT_PATH)
    public ResponseEntity<Map<String, Object>> discovery() {
        Map<String, Object> payload = new LinkedHashMap<>();
        List<Map<String, Object>> endpoints = new ArrayList<>();
        for (ManagementEndpointDescriptor descriptor : endpointRegistry.exposedDescriptors()) {
            endpoints.add(toEndpointView(descriptor));
        }
        payload.put("endpoints", endpoints);
        return ResponseEntity.ok(payload);
    }

    @GetMapping(ManagementPaths.ADMIN_ROOT_PATH + "/{endpointId}")
    public ResponseEntity<?> invoke(@PathVariable("endpointId") String endpointId,
                                    @RequestParam MultiValueMap<String, String> queryParameters) {
        return invoke(endpointId, List.of(), queryParameters);
    }

    @GetMapping(ManagementPaths.ADMIN_ROOT_PATH + "/{endpointId}/{*selector}")
    public ResponseEntity<?> invoke(@PathVariable("endpointId") String endpointId,
                                    @PathVariable("selector") String selector,
                                    @RequestParam MultiValueMap<String, String> queryParameters) {
        return invoke(endpointId, splitSelectors(selector), queryParameters);
    }

    private ResponseEntity<?> invoke(String endpointId,
                                     List<String> selectors,
                                     MultiValueMap<String, String> queryParameters) {
        ManagementEndpoint endpoint = endpointRegistry.resolve(endpointId);
        ManagementResponse response = endpoint.handle(new ManagementRequest(selectors, queryParameters));
        return ResponseEntity.status(response.getStatus())
                .contentType(response.getMediaType())
                .body(response.getBody());
    }

    private Map<String, Object> toEndpointView(ManagementEndpointDescriptor descriptor) {
        Map<String, Object> endpoint = new LinkedHashMap<>();
        endpoint.put("id", descriptor.getId());
        endpoint.put("path", ManagementPaths.adminEndpointPath(descriptor.getId()));
        endpoint.put("description", descriptor.getDescription());
        endpoint.put("produces", descriptor.getProduces());
        endpoint.put("managementAccess", descriptor.getManagementAccess().name());
        if (descriptor.getAliasPath() != null) {
            endpoint.put("aliasPath", descriptor.getAliasPath());
            endpoint.put("aliasAccess", descriptor.getAliasAccess().name());
        }
        return endpoint;
    }

    private List<String> splitSelectors(String selector) {
        List<String> selectors = new ArrayList<>();
        String remaining = selector;
        while (!remaining.isEmpty()) {
            int separator = remaining.indexOf('/');
            if (separator < 0) {
                selectors.add(remaining);
                break;
            }
            String value = remaining.substring(0, separator);
            if (!value.isEmpty()) {
                selectors.add(value);
            }
            remaining = remaining.substring(separator + 1);
        }
        return selectors;
    }
}
