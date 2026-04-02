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

import tech.lamprism.lampray.observability.CounterSpecification;
import tech.lamprism.lampray.observability.DistributionSummarySpecification;
import tech.lamprism.lampray.observability.LongTaskTimerSpecification;
import tech.lamprism.lampray.observability.TagSpecification;
import tech.lamprism.lampray.observability.TimerSpecification;
import tech.lamprism.lampray.security.authentication.login.LoginStrategyType;
import tech.lamprism.lampray.user.Role;

import java.time.Duration;
import java.util.Arrays;

/**
 * @author RollW
 */
public final class AuthenticationMetrics {
    private static final TagSpecification STRATEGY_TAG = TagSpecification.builder("strategy")
            .required()
            .allowedValues(namesOf(LoginStrategyType.values()))
            .build();
    private static final TagSpecification LOGIN_RESULT_TAG = TagSpecification.builder("result")
            .required()
            .allowedValues("success", "failure", "user_not_found", "token_invalid")
            .build();
    private static final TagSpecification TOKEN_SEND_RESULT_TAG = TagSpecification.builder("result")
            .required()
            .allowedValues("success", "failure")
            .build();
    private static final TagSpecification ROLE_TAG = TagSpecification.builder("role")
            .required()
            .allowedValues(namesOf(Role.values()))
            .build();

    public static final CounterSpecification LOGIN_REQUESTS = CounterSpecification.builder(
                    "lampray.auth.login.requests")
            .description("Counts login attempts by strategy and result.")
            .tags(STRATEGY_TAG, LOGIN_RESULT_TAG)
            .build();

    public static final CounterSpecification TOKEN_SEND_REQUESTS = CounterSpecification.builder(
                    "lampray.auth.token.send.requests")
            .description("Counts login token delivery attempts by strategy and result.")
            .tags(STRATEGY_TAG, TOKEN_SEND_RESULT_TAG)
            .build();

    public static final TimerSpecification TOKEN_SEND_DURATION = TimerSpecification.builder(
                    "lampray.auth.token.send.duration")
            .description("Measures end-to-end login token delivery duration.")
            .tag(STRATEGY_TAG)
            .histogram()
            .percentiles(0.5D, 0.95D, 0.99D)
            .serviceLevelObjectives(
                    Duration.ofMillis(100),
                    Duration.ofMillis(300),
                    Duration.ofSeconds(1)
            )
            .build();

    public static final DistributionSummarySpecification TOKEN_SEND_PAYLOAD_SIZE = DistributionSummarySpecification.builder(
                    "lampray.auth.token.send.payload.size")
            .description("Records approximate token payload size before delivery.")
            .tag(STRATEGY_TAG)
            .baseUnit("characters")
            .histogram()
            .percentiles(0.5D, 0.95D, 0.99D)
            .minimumExpectedValue(1D)
            .maximumExpectedValue(4096D)
            .build();

    public static final LongTaskTimerSpecification TOKEN_SEND_LONG_TASK = LongTaskTimerSpecification.builder(
                    "lampray.auth.token.send.long-task")
            .description("Tracks active long-running token delivery operations.")
            .tag(STRATEGY_TAG)
            .build();

    public static final CounterSpecification REGISTRATIONS = CounterSpecification.builder(
                    "lampray.auth.registration.completed")
            .description("Counts successful user registrations by role.")
            .tag(ROLE_TAG)
            .build();

    private AuthenticationMetrics() {
    }

    private static String[] namesOf(Enum<?>[] values) {
        return Arrays.stream(values)
                .map(Enum::name)
                .toArray(String[]::new);
    }
}
