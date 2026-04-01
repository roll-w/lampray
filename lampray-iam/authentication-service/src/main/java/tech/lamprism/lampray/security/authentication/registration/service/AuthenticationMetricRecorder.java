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

package tech.lamprism.lampray.security.authentication.registration.service;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.observability.MetricProvider;
import tech.lamprism.lampray.observability.MetricTask;
import tech.lamprism.lampray.security.authentication.login.LoginStrategyType;
import tech.lamprism.lampray.user.Role;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author RollW
 */
@Component
public final class AuthenticationMetricRecorder {
    private final MetricProvider metricProvider;
    private final Map<LoginStrategyType, AtomicInteger> activeTokenSendTasks = new EnumMap<>(LoginStrategyType.class);

    public AuthenticationMetricRecorder(MetricProvider metricProvider) {
        this.metricProvider = metricProvider;
        initializeActiveTokenSendGauges();
    }

    public void recordLoginResult(LoginStrategyType type,
                                  String result) {
        metricProvider.increment(AuthenticationMetrics.LOGIN_REQUESTS, loginTags(type, result));
    }

    public void recordTokenPayloadSize(LoginStrategyType type,
                                       int payloadSize) {
        metricProvider.recordValue(AuthenticationMetrics.TOKEN_SEND_PAYLOAD_SIZE, strategyTags(type), payloadSize);
    }

    public void recordTokenSendResult(LoginStrategyType type,
                                      Duration duration,
                                      String result) {
        metricProvider.recordDuration(AuthenticationMetrics.TOKEN_SEND_DURATION, strategyTags(type), duration);
        metricProvider.increment(AuthenticationMetrics.TOKEN_SEND_REQUESTS, tokenSendTags(type, result));
    }

    public TokenSendTask startTokenSendTask(LoginStrategyType type) {
        AtomicInteger activeTasks = activeTokenSendTasks.get(type);
        if (activeTasks == null) {
            throw new IllegalArgumentException("No active task gauge for type: " + type);
        }
        activeTasks.incrementAndGet();
        return new TokenSendTask(activeTasks, metricProvider.startLongTask(AuthenticationMetrics.TOKEN_SEND_LONG_TASK, strategyTags(type)));
    }

    public void recordRegistration(Role role) {
        metricProvider.increment(AuthenticationMetrics.REGISTRATIONS, registrationTags(role));
    }

    private void initializeActiveTokenSendGauges() {
        for (LoginStrategyType type : LoginStrategyType.values()) {
            AtomicInteger activeTasks = new AtomicInteger();
            activeTokenSendTasks.put(type, activeTasks);
            metricProvider.gauge(AuthenticationMetrics.ACTIVE_TOKEN_SEND_TASKS, strategyTags(type), activeTasks, AtomicInteger::doubleValue);
        }
    }

    private Map<String, String> loginTags(LoginStrategyType type,
                                          String result) {
        return Map.of("strategy", type.name(), "result", result);
    }

    private Map<String, String> tokenSendTags(LoginStrategyType type,
                                              String result) {
        return Map.of("strategy", type.name(), "result", result);
    }

    private Map<String, String> strategyTags(LoginStrategyType type) {
        return Map.of("strategy", type.name());
    }

    private Map<String, String> registrationTags(Role role) {
        return Map.of("role", role.name());
    }

    /**
     * @author RollW
     */
    public static final class TokenSendTask implements AutoCloseable {
        private final AtomicInteger activeTasks;
        private final MetricTask metricTask;
        private boolean closed;

        private TokenSendTask(AtomicInteger activeTasks,
                              MetricTask metricTask) {
            this.activeTasks = activeTasks;
            this.metricTask = metricTask;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            activeTasks.updateAndGet(current -> Math.max(0, current - 1));
            metricTask.close();
        }
    }
}
