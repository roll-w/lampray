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

import tech.lamprism.lampray.observability.DefaultMetricSpecification;
import tech.lamprism.lampray.observability.MetricSpecification;
import tech.lamprism.lampray.observability.MetricType;

import java.time.Duration;
import java.util.List;

/**
 * @author RollW
 */
public final class AuthenticationMetrics {
    public static final MetricSpecification LOGIN_REQUESTS = DefaultMetricSpecification.builder(
                    "lampray.auth.login.requests",
                    MetricType.COUNTER)
            .description("Counts login attempts by strategy and result.")
            .allowTags("strategy", "result")
            .build();

    public static final MetricSpecification TOKEN_SEND_REQUESTS = DefaultMetricSpecification.builder(
                    "lampray.auth.token.send.requests",
                    MetricType.COUNTER)
            .description("Counts login token delivery attempts by strategy and result.")
            .allowTags("strategy", "result")
            .build();

    public static final MetricSpecification TOKEN_SEND_DURATION = DefaultMetricSpecification.builder(
                    "lampray.auth.token.send.duration",
                    MetricType.TIMER)
            .description("Measures end-to-end login token delivery duration.")
            .allowTags("strategy")
            .histogram()
            .percentiles(0.5D, 0.95D, 0.99D)
            .serviceLevelObjectives(
                    Duration.ofMillis(100).toNanos(),
                    Duration.ofMillis(300).toNanos(),
                    Duration.ofSeconds(1).toNanos()
            )
            .build();

    public static final MetricSpecification TOKEN_SEND_PAYLOAD_SIZE = DefaultMetricSpecification.builder(
                    "lampray.auth.token.send.payload.size",
                    MetricType.HISTOGRAM)
            .description("Records approximate token payload size before delivery.")
            .allowTags("strategy")
            .baseUnit("characters")
            .histogram()
            .percentiles(0.5D, 0.95D, 0.99D)
            .minimumExpectedValue(1D)
            .maximumExpectedValue(4096D)
            .build();

    public static final MetricSpecification TOKEN_SEND_LONG_TASK = DefaultMetricSpecification.builder(
                    "lampray.auth.token.send.long-task",
                    MetricType.LONG_TASK_TIMER)
            .description("Tracks active long-running token delivery operations.")
            .allowTags("strategy")
            .build();

    public static final MetricSpecification ACTIVE_TOKEN_SEND_TASKS = DefaultMetricSpecification.builder(
                    "lampray.auth.token.send.active",
                    MetricType.GAUGE)
            .description("Current number of active token delivery tasks.")
            .allowTags("strategy")
            .build();

    public static final MetricSpecification REGISTRATIONS = DefaultMetricSpecification.builder(
                    "lampray.auth.registration.completed",
                    MetricType.COUNTER)
            .description("Counts successful user registrations by role.")
            .allowTags("role")
            .build();

    public static final List<MetricSpecification> SPECIFICATIONS = List.of(
            LOGIN_REQUESTS,
            TOKEN_SEND_REQUESTS,
            TOKEN_SEND_DURATION,
            TOKEN_SEND_PAYLOAD_SIZE,
            TOKEN_SEND_LONG_TASK,
            ACTIVE_TOKEN_SEND_TASKS,
            REGISTRATIONS
    );

    private AuthenticationMetrics() {
    }
}
