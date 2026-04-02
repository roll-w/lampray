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

import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.CommonRuntimeException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author RollW
 */
@Component
public class MetricsManagementEndpoint implements ManagementEndpoint {
    private static final ManagementEndpointDescriptor DESCRIPTOR = new ManagementEndpointDescriptor(
            ManagementEndpointIds.METRICS,
            "Application metrics metadata and samples",
            MediaType.APPLICATION_JSON_VALUE,
            ManagementAccess.ADMIN,
            null,
            null
    );

    private final MeterRegistry meterRegistry;

    public MetricsManagementEndpoint(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ManagementEndpointDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public ManagementResponse handle(ManagementRequest request) {
        if (request.getSelectorCount() == 0) {
            String prefix = request.getQueryParameter("prefix").stream().findFirst().orElse(null);
            return ManagementResponse.okJson(listMetrics(prefix));
        }
        if (request.getSelectorCount() == 1) {
            return ManagementResponse.okJson(metricDetails(
                    request.getSelector(0),
                    request.getQueryParameter("tag")
            ));
        }
        throw new CommonRuntimeException(CommonErrorCode.ERROR_NOT_FOUND);
    }

    private List<Map<String, Object>> listMetrics(String prefix) {
        Map<String, List<Meter>> grouped = new LinkedHashMap<>();
        meterRegistry.getMeters().stream()
                .filter(meter -> prefix == null || meter.getId().getName().startsWith(prefix))
                .sorted(Comparator.comparing(meter -> meter.getId().getName()))
                .forEach(meter -> grouped.computeIfAbsent(meter.getId().getName(), key -> new ArrayList<>()).add(meter));

        List<Map<String, Object>> payload = new ArrayList<>();
        grouped.forEach((name, meters) -> payload.add(toMeterSummary(name, meters)));
        return payload;
    }

    private Map<String, Object> metricDetails(String name,
                                              List<String> tagFilters) {
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
        return details;
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
}
