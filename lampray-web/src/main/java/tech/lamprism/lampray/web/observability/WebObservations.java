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

package tech.lamprism.lampray.web.observability;

import tech.lamprism.lampray.observability.DefaultObservationSpecification;
import tech.lamprism.lampray.observability.ObservationSpecification;
import tech.lamprism.lampray.observability.TagSpecification;

/**
 * @author RollW
 */
public final class WebObservations {
    public static final ObservationSpecification HTTP_SERVER_REQUEST = DefaultObservationSpecification.system(
                    "lampray.http.server.request")
            .tags(
                    TagSpecification.builder("method").build(),
                    TagSpecification.builder("uri").build(),
                    TagSpecification.builder("status").build(),
                    TagSpecification.builder("result")
                            .allowedValues("success", "redirect", "client_error", "server_error", "error")
                            .build()
            )
            .build();

    public static final ObservationSpecification ASYNC_TASK = DefaultObservationSpecification.system(
                    "lampray.async.task")
            .tags(
                    TagSpecification.builder("executor")
                            .allowedValues("main")
                            .build(),
                    TagSpecification.builder("result")
                            .allowedValues("success", "failure")
                            .build()
            )
            .build();

    private WebObservations() {
    }
}
