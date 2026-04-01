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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author RollW
 */
@RestController
public class ObservabilitySystemProbeController {
    private final ObservabilityController observabilityController;

    public ObservabilitySystemProbeController(ObservabilityController observabilityController) {
        this.observabilityController = observabilityController;
    }

    @GetMapping("/healthz")
    public ResponseEntity<Map<String, Object>> getHealth() {
        return sanitizeHealth(observabilityController.getHealth());
    }

    @GetMapping(value = "/metrics", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getPrometheusMetrics() {
        return observabilityController.scrapePrometheus();
    }

    private ResponseEntity<Map<String, Object>> sanitizeHealth(ResponseEntity<Map<String, Object>> response) {
        Map<String, Object> source = response.getBody();
        if (source == null) {
            return response;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", source.get("status"));
        return ResponseEntity.status(response.getStatusCode()).body(payload);
    }
}
