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

import tech.lamprism.lampray.observability.DefaultObservationSpecification;
import tech.lamprism.lampray.observability.ObservationSpecification;
import tech.lamprism.lampray.observability.TagSpecification;
import tech.lamprism.lampray.security.authentication.login.LoginStrategyType;

import java.util.Arrays;

/**
 * @author RollW
 */
public final class AuthenticationObservations {
    public static final ObservationSpecification LOGIN = DefaultObservationSpecification.business("lampray.auth.login")
            .tags(
                    TagSpecification.builder("strategy")
                            .allowedValues(namesOf(LoginStrategyType.values()))
                            .build(),
                    TagSpecification.builder("result")
                            .allowedValues("success", "failure", "user_not_found", "token_invalid")
                            .build()
            )
            .build();

    private AuthenticationObservations() {
    }

    private static String[] namesOf(Enum<?>[] values) {
        return Arrays.stream(values)
                .map(Enum::name)
                .toArray(String[]::new);
    }
}
