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
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * @author RollW
 */
@Component
public class AsyncExecutorMetricsBinder implements SmartInitializingSingleton {
    private final MeterRegistry meterRegistry;
    private final ThreadPoolTaskExecutor mainScheduledExecutorService;

    public AsyncExecutorMetricsBinder(MeterRegistry meterRegistry,
                                      ThreadPoolTaskExecutor mainScheduledExecutorService) {
        this.meterRegistry = meterRegistry;
        this.mainScheduledExecutorService = mainScheduledExecutorService;
    }

    @Override
    public void afterSingletonsInstantiated() {
        ExecutorServiceMetrics.monitor(
                meterRegistry,
                mainScheduledExecutorService.getThreadPoolExecutor(),
                "lampray.async.executor"
        );
    }
}
