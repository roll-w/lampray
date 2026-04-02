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

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.Version;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.web.common.keys.ObservabilityConfigKeys;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.CommonRuntimeException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author RollW
 */
@Component
public class InfoManagementEndpoint implements ManagementEndpoint {
    private static final ManagementEndpointDescriptor DESCRIPTOR = new ManagementEndpointDescriptor(
            ManagementEndpointIds.INFO,
            "Observability information",
            MediaType.APPLICATION_JSON_VALUE,
            ManagementAccess.ADMIN,
            null,
            null
    );

    private final MeterRegistry meterRegistry;
    private final ObservationRegistry observationRegistry;
    private final ConfigReader configReader;
    private final ManagementExposurePolicy exposurePolicy;

    public InfoManagementEndpoint(MeterRegistry meterRegistry,
                                  ObservationRegistry observationRegistry,
                                  ConfigReader configReader,
                                  ManagementExposurePolicy exposurePolicy) {
        this.meterRegistry = meterRegistry;
        this.observationRegistry = observationRegistry;
        this.configReader = configReader;
        this.exposurePolicy = exposurePolicy;
    }

    @Override
    public ManagementEndpointDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public ManagementResponse handle(ManagementRequest request) {
        if (request.getSelectorCount() > 0) {
            throw new CommonRuntimeException(CommonErrorCode.ERROR_NOT_FOUND);
        }
        return ManagementResponse.okJson(buildPayload());
    }

    private Map<String, Object> buildPayload() {
        Map<String, Object> info = new LinkedHashMap<>();
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
        info.put("exposedEndpoints", exposurePolicy.exposedEndpoints());
        info.put("meterCount", meterRegistry.getMeters().size());
        info.put("meterTypes", summarizeMeterTypes());
        return info;
    }

    private Map<String, Integer> summarizeMeterTypes() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Meter meter : meterRegistry.getMeters()) {
            String type = meter.getId().getType().name();
            counts.merge(type, 1, Integer::sum);
        }
        return counts;
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
