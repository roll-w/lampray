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

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.observability.Health;
import tech.lamprism.lampray.observability.HealthGroupSpecification;
import tech.lamprism.lampray.observability.core.CompositeHealthService;
import tech.lamprism.lampray.web.observability.health.HealthHttpStatusMapper;
import tech.lamprism.lampray.web.observability.health.ObservabilityHealthGroups;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.CommonRuntimeException;

/**
 * @author RollW
 */
@Component
public class HealthManagementEndpoint implements ManagementEndpoint {
    private static final ManagementEndpointDescriptor DESCRIPTOR = new ManagementEndpointDescriptor(
            ManagementEndpointIds.HEALTH,
            "Aggregated application health",
            MediaType.APPLICATION_JSON_VALUE,
            ManagementAccess.ADMIN,
            ManagementPaths.HEALTH_PROBE_PATH,
            ManagementAccess.PUBLIC
    );

    private final CompositeHealthService compositeHealthService;
    private final HealthHttpStatusMapper healthHttpStatusMapper;
    private final ObservabilityHealthGroups healthGroups;

    public HealthManagementEndpoint(CompositeHealthService compositeHealthService,
                                    HealthHttpStatusMapper healthHttpStatusMapper,
                                    ObservabilityHealthGroups healthGroups) {
        this.compositeHealthService = compositeHealthService;
        this.healthHttpStatusMapper = healthHttpStatusMapper;
        this.healthGroups = healthGroups;
    }

    @Override
    public ManagementEndpointDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public ManagementResponse handle(ManagementRequest request) {
        Health health;
        if (request.getSelectorCount() == 0) {
            health = compositeHealthService.overall(healthGroups.all());
        } else if (request.getSelectorCount() == 1) {
            HealthGroupSpecification specification = healthGroups.group(request.getSelector(0));
            if (specification == null) {
                throw new CommonRuntimeException(CommonErrorCode.ERROR_NOT_FOUND);
            }
            health = compositeHealthService.group(specification);
        } else {
            throw new CommonRuntimeException(CommonErrorCode.ERROR_NOT_FOUND);
        }
        return ManagementResponse.status(
                healthHttpStatusMapper.map(health.statusValue()),
                MediaType.APPLICATION_JSON,
                health
        );
    }
}
