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

package tech.lamprism.lampray.web.controller.storage.admin;

import org.springframework.web.bind.annotation.GetMapping;
import tech.lamprism.lampray.storage.monitoring.StorageBackendMonitoringView;
import tech.lamprism.lampray.storage.monitoring.StorageGroupMonitoringView;
import tech.lamprism.lampray.storage.monitoring.StorageMonitoringOverview;
import tech.lamprism.lampray.storage.monitoring.StorageMonitoringService;
import tech.lamprism.lampray.web.controller.AdminApi;
import tech.rollw.common.web.HttpResponseEntity;

import java.util.List;

@AdminApi
public class StorageMonitoringController {
    private final StorageMonitoringService storageMonitoringService;

    public StorageMonitoringController(StorageMonitoringService storageMonitoringService) {
        this.storageMonitoringService = storageMonitoringService;
    }

    @GetMapping("/storage/monitoring/overview")
    public HttpResponseEntity<StorageMonitoringOverview> getOverview() {
        return HttpResponseEntity.success(storageMonitoringService.getOverview());
    }

    @GetMapping("/storage/monitoring/backends")
    public HttpResponseEntity<List<StorageBackendMonitoringView>> listBackendMonitoring() {
        return HttpResponseEntity.success(storageMonitoringService.listBackendMonitoring());
    }

    @GetMapping("/storage/monitoring/groups")
    public HttpResponseEntity<List<StorageGroupMonitoringView>> listGroupMonitoring() {
        return HttpResponseEntity.success(storageMonitoringService.listGroupMonitoring());
    }
}
