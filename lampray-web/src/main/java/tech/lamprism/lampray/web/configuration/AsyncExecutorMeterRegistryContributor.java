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

package tech.lamprism.lampray.web.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.observability.MeterRegistryContributor;

/**
 * @author RollW
 */
@Component
public class AsyncExecutorMeterRegistryContributor implements MeterRegistryContributor {
    private final ThreadPoolTaskExecutor mainScheduledExecutorService;

    public AsyncExecutorMeterRegistryContributor(ThreadPoolTaskExecutor mainScheduledExecutorService) {
        this.mainScheduledExecutorService = mainScheduledExecutorService;
    }

    @Override
    public int getOrder() {
        return 200;
    }

    @Override
    public void contribute(MeterRegistry meterRegistry) {
        ExecutorServiceMetrics.monitor(
                meterRegistry,
                mainScheduledExecutorService.getThreadPoolExecutor(),
                "lampray.async.executor"
        );
    }
}
