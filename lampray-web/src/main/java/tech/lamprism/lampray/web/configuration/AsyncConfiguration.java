/*
 * Copyright (C) 2023-2025 RollW
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

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import space.lingu.NonNull;
import tech.lamprism.lampray.server.AddressProvider;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author RollW
 */
@EnableAsync(proxyTargetClass = true)
@Configuration
public class AsyncConfiguration implements AsyncConfigurer {
    private final AddressProvider addressProvider;

    public AsyncConfiguration(AddressProvider addressProvider) {
        this.addressProvider = addressProvider;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

    @Override
    public Executor getAsyncExecutor() {
        return mainScheduledExecutorService();
    }

    @Bean
    public ThreadPoolTaskExecutor mainScheduledExecutorService() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int cpuCount = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(Math.max(2, cpuCount * 2));
        executor.setMaxPoolSize(Math.max(4, cpuCount * 4));
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("async-main-");
        executor.setTaskDecorator(new AddressContextTaskDecorator(addressProvider));

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    /**
     * Task decorator that automatically manages address context.
     */
    private static class AddressContextTaskDecorator implements TaskDecorator {

        private final AddressProvider addressProvider;

        public AddressContextTaskDecorator(AddressProvider addressProvider) {
            this.addressProvider = addressProvider;
        }

        @NonNull
        @Override
        public Runnable decorate(@NonNull Runnable runnable) {
            // Capture context in the calling thread
            AddressProvider.AsyncContext context = addressProvider.captureContext();

            return () -> {
                if (context != null) {
                    addressProvider.setAsyncContext(context);
                }
                try {
                    runnable.run();
                } finally {
                    addressProvider.clearAsyncContext();
                }
            };
        }
    }

}
