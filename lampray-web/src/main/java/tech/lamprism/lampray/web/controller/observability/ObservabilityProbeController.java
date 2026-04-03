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

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.lamprism.lampray.observability.Health;
import tech.lamprism.lampray.web.observability.management.ManagementEndpointIds;
import tech.lamprism.lampray.web.observability.management.ManagementEndpointRegistry;
import tech.lamprism.lampray.web.observability.management.ManagementPaths;
import tech.lamprism.lampray.web.observability.management.ManagementRequest;
import tech.lamprism.lampray.web.observability.management.ManagementResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author RollW
 */
@RestController
public class ObservabilityProbeController {
    private final ManagementEndpointRegistry endpointRegistry;

    public ObservabilityProbeController(ManagementEndpointRegistry endpointRegistry) {
        this.endpointRegistry = endpointRegistry;
    }

    @GetMapping(ManagementPaths.HEALTH_PROBE_PATH)
    public ResponseEntity<Map<String, Object>> health() {
        if (!endpointRegistry.isExposed(ManagementEndpointIds.HEALTH)) {
            return ResponseEntity.notFound().build();
        }

        ManagementResponse response = endpointRegistry.resolve(ManagementEndpointIds.HEALTH).handle(
                new ManagementRequest(List.of("liveness"), Map.of())
        );
        Object body = response.getBody();
        if (!(body instanceof Health health)) {
            return ResponseEntity.status(response.getStatus()).build();
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", health.getStatus());
        return ResponseEntity.status(response.getStatus()).body(payload);
    }

    @GetMapping(value = ManagementPaths.METRICS_PROBE_PATH, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> metrics() {
        if (!endpointRegistry.isExposed(ManagementEndpointIds.PROMETHEUS)) {
            return ResponseEntity.notFound().build();
        }

        ManagementResponse response = endpointRegistry.resolve(ManagementEndpointIds.PROMETHEUS).handle(ManagementRequest.empty());
        if (!response.getStatus().is2xxSuccessful()) {
            return ResponseEntity.status(response.getStatus()).build();
        }
        return ResponseEntity.status(response.getStatus())
                .contentType(MediaType.TEXT_PLAIN)
                .body(String.valueOf(response.getBody()));
    }
}
