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

import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import tech.lamprism.lampray.Version;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.web.controller.AdminApi;
import tech.lamprism.lampray.web.common.keys.ObservabilityConfigKeys;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.CommonRuntimeException;
import tech.rollw.common.web.HttpResponseEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

/**
 * @author RollW
 */
@AdminApi
public class ObservabilityController {
    private final MeterRegistry meterRegistry;
    private final ObservationRegistry observationRegistry;
    private final ConfigReader configReader;
    private final ObjectProvider<DataSource> dataSourceProvider;

    public ObservabilityController(MeterRegistry meterRegistry,
                                   ObservationRegistry observationRegistry,
                                   ConfigReader configReader,
                                   ObjectProvider<DataSource> dataSourceProvider) {
        this.meterRegistry = meterRegistry;
        this.observationRegistry = observationRegistry;
        this.configReader = configReader;
        this.dataSourceProvider = dataSourceProvider;
    }

    @GetMapping("/observability")
    public HttpResponseEntity<Map<String, Object>> getObservabilityStatus() {
        return HttpResponseEntity.success(buildInfoPayload());
    }

    @GetMapping("/observability/info")
    public HttpResponseEntity<Map<String, Object>> getObservabilityInfo() {
        return HttpResponseEntity.success(buildInfoPayload());
    }

    @GetMapping("/observability/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        return createHealthResponse(buildOverallHealth());
    }

    @GetMapping("/observability/health/liveness")
    public ResponseEntity<Map<String, Object>> getLiveness() {
        return createHealthResponse(buildLivenessHealth());
    }

    @GetMapping("/observability/health/readiness")
    public ResponseEntity<Map<String, Object>> getReadiness() {
        return createHealthResponse(buildReadinessHealth());
    }

    @GetMapping("/observability/metrics")
    public HttpResponseEntity<List<Map<String, Object>>> listMetrics(
            @RequestParam(name = "prefix", required = false) String prefix
    ) {
        Map<String, List<Meter>> grouped = new LinkedHashMap<>();
        meterRegistry.getMeters().stream()
                .filter(meter -> prefix == null || meter.getId().getName().startsWith(prefix))
                .sorted(Comparator.comparing(meter -> meter.getId().getName()))
                .forEach(meter -> grouped.computeIfAbsent(meter.getId().getName(), key -> new ArrayList<>()).add(meter));

        List<Map<String, Object>> payload = new ArrayList<>();
        grouped.forEach((name, meters) -> payload.add(toMeterSummary(name, meters)));
        return HttpResponseEntity.success(payload);
    }

    @GetMapping("/observability/metrics/{name}")
    public HttpResponseEntity<Map<String, Object>> getMetricDetails(
            @PathVariable("name") String name,
            @RequestParam(name = "tag", required = false) List<String> tagFilters
    ) {
        var search = meterRegistry.find(name);
        for (Tag tag : parseTags(tagFilters)) {
            search = search.tag(tag.getKey(), tag.getValue());
        }

        List<Meter> meters = search.meters();
        if (meters.isEmpty()) {
            throw new CommonRuntimeException(CommonErrorCode.ERROR_NOT_FOUND);
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("name", name);
        details.put("count", meters.size());
        details.put("availableTags", toAvailableTags(meters));

        List<Map<String, Object>> meterViews = new ArrayList<>();
        for (Meter meter : meters) {
            meterViews.add(toMeterDetails(meter));
        }
        details.put("meters", meterViews);
        return HttpResponseEntity.success(details);
    }

    @GetMapping(value = "/observability/prometheus", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> scrapePrometheus() {
        if (!isPrometheusEnabled() || !(meterRegistry instanceof PrometheusMeterRegistry prometheusMeterRegistry)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(prometheusMeterRegistry.scrape());
    }

    private Map<String, Integer> summarizeMeterTypes() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Meter meter : meterRegistry.getMeters()) {
            String type = meter.getId().getType().name();
            counts.merge(type, 1, Integer::sum);
        }
        return counts;
    }

    private Map<String, Object> toMeterSummary(String name,
                                               List<Meter> meters) {
        Meter.Id id = meters.get(0).getId();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("name", name);
        summary.put("type", id.getType().name());
        summary.put("baseUnit", id.getBaseUnit());
        summary.put("description", id.getDescription());
        summary.put("variants", meters.size());

        Set<String> tagKeys = new LinkedHashSet<>();
        for (Meter meter : meters) {
            for (Tag tag : meter.getId().getTags()) {
                tagKeys.add(tag.getKey());
            }
        }
        summary.put("tagKeys", tagKeys);
        return summary;
    }

    private Map<String, List<String>> toAvailableTags(List<Meter> meters) {
        Map<String, Set<String>> tags = new LinkedHashMap<>();
        for (Meter meter : meters) {
            for (Tag tag : meter.getId().getTags()) {
                tags.computeIfAbsent(tag.getKey(), key -> new LinkedHashSet<>()).add(tag.getValue());
            }
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        tags.forEach((key, value) -> result.put(key, new ArrayList<>(value)));
        return result;
    }

    private Map<String, Object> toMeterDetails(Meter meter) {
        Map<String, Object> details = new LinkedHashMap<>();
        Meter.Id id = meter.getId();
        details.put("name", id.getName());
        details.put("type", id.getType().name());
        details.put("baseUnit", id.getBaseUnit());
        details.put("description", id.getDescription());
        details.put("tags", toTagMap(id.getTags()));

        List<Map<String, Object>> measurements = new ArrayList<>();
        for (Measurement measurement : meter.measure()) {
            Map<String, Object> measurementView = new LinkedHashMap<>();
            measurementView.put("statistic", measurement.getStatistic().name());
            measurementView.put("value", measurement.getValue());
            measurements.add(measurementView);
        }
        details.put("measurements", measurements);
        return details;
    }

    private Map<String, String> toTagMap(List<Tag> tags) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Tag tag : tags) {
            result.put(tag.getKey(), tag.getValue());
        }
        return result;
    }

    private List<Tag> parseTags(List<String> tagFilters) {
        if (tagFilters == null || tagFilters.isEmpty()) {
            return List.of();
        }

        List<Tag> tags = new ArrayList<>();
        for (String tagFilter : tagFilters) {
            int separator = tagFilter.indexOf(':');
            if (separator <= 0 || separator == tagFilter.length() - 1) {
                throw new CommonRuntimeException(
                        CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                        "Tag filter must use key:value syntax"
                );
            }
            tags.add(Tag.of(tagFilter.substring(0, separator), tagFilter.substring(separator + 1)));
        }
        return tags;
    }

    private boolean isPrometheusEnabled() {
        return Boolean.TRUE.equals(configReader.get(ObservabilityConfigKeys.PROMETHEUS_ENABLED, true));
    }

    private Map<String, Object> buildInfoPayload() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("prometheusEnabled", isPrometheusEnabled());
        info.put("registryType", meterRegistry.getClass().getName());
        info.put("observationRegistryType", observationRegistry.getClass().getName());
        info.put("version", Version.VERSION);
        info.put("buildTime", Version.BUILD_TIME);
        info.put("commitId", Version.GIT_COMMIT_ID_ABBREV);
        info.put("gitCommitTime", Version.GIT_COMMIT_TIME);
        info.put("javaRuntimeVersion", Version.JAVA_RUNTIME_VERSION);
        info.put("javaVmName", Version.JAVA_VM_NAME);
        info.put("requestIdHeader", configReader.get(
                ObservabilityConfigKeys.REQUEST_ID_HEADER,
                ObservabilityConfigKeys.DEFAULT_REQUEST_ID_HEADER
        ));
        info.put("commonTags", commonTags());
        info.put("meterCount", meterRegistry.getMeters().size());
        info.put("meterTypes", summarizeMeterTypes());
        return info;
    }

    private ResponseEntity<Map<String, Object>> createHealthResponse(Map<String, Object> health) {
        String status = String.valueOf(health.get("status"));
        HttpStatus httpStatus = "UP".equals(status) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(health);
    }

    private Map<String, Object> buildOverallHealth() {
        Map<String, Object> liveness = buildLivenessHealth();
        Map<String, Object> readiness = buildReadinessHealth();
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("liveness", liveness);
        components.put("readiness", readiness);

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", aggregateStatus(List.of(liveness, readiness)));
        health.put("components", components);
        return health;
    }

    private Map<String, Object> buildLivenessHealth() {
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("application", healthComponent("UP", Map.of(
                "version", Version.VERSION,
                "buildTime", Version.BUILD_TIME
        )));
        components.put("observationRegistry", healthComponent("UP", Map.of(
                "type", observationRegistry.getClass().getName()
        )));
        components.put("meterRegistry", healthComponent("UP", Map.of(
                "type", meterRegistry.getClass().getName(),
                "meterCount", meterRegistry.getMeters().size()
        )));

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", aggregateStatus(List.copyOf(components.values())));
        health.put("components", components);
        return health;
    }

    private Map<String, Object> buildReadinessHealth() {
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("observability", healthComponent("UP", Map.of(
                "prometheusEnabled", isPrometheusEnabled()
        )));
        components.put("database", buildDatabaseHealth());
        components.put("prometheus", buildPrometheusHealth());

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", aggregateStatus(List.copyOf(components.values())));
        health.put("components", components);
        return health;
    }

    private Map<String, Object> buildDatabaseHealth() {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            return healthComponent("DOWN", Map.of("reason", "No DataSource bean available"));
        }

        try (var connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(1);
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("valid", valid);
            details.put("product", connection.getMetaData().getDatabaseProductName());
            details.put("url", connection.getMetaData().getURL());
            return healthComponent(valid ? "UP" : "DOWN", details);
        } catch (Exception ex) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("message", ex.getMessage() == null ? ex.getClass().getName() : ex.getMessage());
            return healthComponent("DOWN", details);
        }
    }

    private Map<String, Object> buildPrometheusHealth() {
        if (!isPrometheusEnabled()) {
            return healthComponent("UP", Map.of(
                    "enabled", false,
                    "reason", "Endpoint disabled by config"
            ));
        }

        boolean available = meterRegistry instanceof PrometheusMeterRegistry;
        return healthComponent(available ? "UP" : "DOWN", Map.of(
                "enabled", true,
                "registryType", meterRegistry.getClass().getName()
        ));
    }

    private String aggregateStatus(Iterable<?> components) {
        for (Object componentObject : components) {
            if (!(componentObject instanceof Map<?, ?> component)) {
                continue;
            }
            if (!"UP".equals(component.get("status"))) {
                return "DOWN";
            }
        }
        return "UP";
    }

    private Map<String, Object> healthComponent(String status,
                                                Map<String, ?> details) {
        Map<String, Object> component = new LinkedHashMap<>();
        component.put("status", status);
        component.put("details", details);
        return component;
    }

    private Map<String, String> commonTags() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("application", configReader.get(
                ObservabilityConfigKeys.COMMON_TAG_APPLICATION,
                ObservabilityConfigKeys.DEFAULT_APPLICATION_TAG
        ));
        tags.put("instance", configReader.get(
                ObservabilityConfigKeys.COMMON_TAG_INSTANCE,
                ObservabilityConfigKeys.DEFAULT_INSTANCE_TAG
        ));
        tags.put("environment", configReader.get(
                ObservabilityConfigKeys.COMMON_TAG_ENVIRONMENT,
                ObservabilityConfigKeys.DEFAULT_ENVIRONMENT_TAG
        ));
        return tags;
    }
}
