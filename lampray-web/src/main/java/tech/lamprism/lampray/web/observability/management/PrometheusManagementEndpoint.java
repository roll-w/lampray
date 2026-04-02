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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.CommonRuntimeException;

/**
 * @author RollW
 */
@Component
public class PrometheusManagementEndpoint implements ManagementEndpoint {
    private static final ManagementEndpointDescriptor DESCRIPTOR = new ManagementEndpointDescriptor(
            ManagementEndpointIds.PROMETHEUS,
            "Prometheus scrape output",
            MediaType.TEXT_PLAIN_VALUE,
            ManagementAccess.ADMIN,
            ManagementPaths.METRICS_PROBE_PATH,
            ManagementAccess.TOKEN
    );

    private final MeterRegistry meterRegistry;

    public PrometheusManagementEndpoint(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
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
        if (!(meterRegistry instanceof PrometheusMeterRegistry prometheusMeterRegistry)) {
            return ManagementResponse.status(HttpStatus.NOT_FOUND, MediaType.TEXT_PLAIN, "");
        }
        return ManagementResponse.okText(prometheusMeterRegistry.scrape());
    }
}
