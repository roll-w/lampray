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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import space.lingu.NonNull;
import tech.lamprism.lampray.observability.CorrelationContext;
import tech.lamprism.lampray.observability.CorrelationContextHolder;
import tech.lamprism.lampray.observability.ObservationScope;
import tech.lamprism.lampray.observability.Observations;
import tech.lamprism.lampray.observability.SignalTags;
import tech.lamprism.lampray.server.AutoInferredAddressProvider;
import tech.lamprism.lampray.web.observability.CorrelationMdcSupport;
import tech.lamprism.lampray.web.observability.WebObservations;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author RollW
 */
@EnableAsync(proxyTargetClass = true)
@Configuration
public class AsyncConfiguration implements AsyncConfigurer {
    private final AutoInferredAddressProvider autoInferredAddressProvider;
    private final CorrelationContextHolder correlationContextHolder;
    private final ObjectProvider<Observations> observationsProvider;

    public AsyncConfiguration(AutoInferredAddressProvider autoInferredAddressProvider,
                              CorrelationContextHolder correlationContextHolder,
                              ObjectProvider<Observations> observationsProvider) {
        this.autoInferredAddressProvider = autoInferredAddressProvider;
        this.correlationContextHolder = correlationContextHolder;
        this.observationsProvider = observationsProvider;
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
        executor.setCorePoolSize(Math.max(2, Math.min(16, cpuCount * 2)));
        executor.setMaxPoolSize(Math.max(4, Math.min(32, cpuCount * 4)));
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("async-main-");
        executor.setTaskDecorator(new AddressContextTaskDecorator(
                autoInferredAddressProvider,
                correlationContextHolder,
                observationsProvider
        ));

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    /**
     * Task decorator that automatically manages address context.
     */
    /**
     * @author RollW
     */
    private static class AddressContextTaskDecorator implements TaskDecorator {

        private final AutoInferredAddressProvider autoInferredAddressProvider;
        private final CorrelationContextHolder correlationContextHolder;
        private final ObjectProvider<Observations> observationsProvider;

        public AddressContextTaskDecorator(AutoInferredAddressProvider autoInferredAddressProvider,
                                           CorrelationContextHolder correlationContextHolder,
                                           ObjectProvider<Observations> observationsProvider) {
            this.autoInferredAddressProvider = autoInferredAddressProvider;
            this.correlationContextHolder = correlationContextHolder;
            this.observationsProvider = observationsProvider;
        }

        @NonNull
        @Override
        public Runnable decorate(@NonNull Runnable runnable) {
            AutoInferredAddressProvider.AsyncContext context = autoInferredAddressProvider.captureContext();
            CorrelationContext correlationContext = correlationContextHolder.current();

            return () -> {
                CorrelationContext previousCorrelationContext = correlationContextHolder.swap(correlationContext);
                CorrelationMdcSupport.replace(correlationContext);
                ObservationScope scope = observationsProvider.getObject().open(
                        WebObservations.ASYNC_TASK,
                        SignalTags.of("executor", "main")
                );
                if (context != null) {
                    autoInferredAddressProvider.setAsyncContext(context);
                }
                try {
                    runnable.run();
                    scope.tag("result", "success");
                } catch (RuntimeException | Error ex) {
                    scope.tag("result", "failure");
                    scope.error(ex);
                    throw ex;
                } finally {
                    scope.close();
                    correlationContextHolder.restore(previousCorrelationContext);
                    CorrelationMdcSupport.replace(previousCorrelationContext);
                    autoInferredAddressProvider.clearAsyncContext();
                }
            };
        }
    }

}
