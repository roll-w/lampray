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

package tech.lamprism.lampray.content.article.service;

import tech.lamprism.lampray.observability.CounterSpecification;
import tech.lamprism.lampray.observability.TagSpecification;
import tech.lamprism.lampray.observability.TimerSpecification;

import java.time.Duration;

/**
 * @author RollW
 */
public final class ArticleMetrics {
    public static final CounterSpecification ARTICLE_PUBLISH_REQUESTS = CounterSpecification.builder(
                    "lampray.article.publish.requests")
            .description("Counts article publish attempts by result.")
            .tag(TagSpecification.builder("result")
                    .required()
                    .allowedValues("success", "invalid_type", "duplicated")
                    .build())
            .build();

    public static final TimerSpecification ARTICLE_PUBLISH_DURATION = TimerSpecification.builder(
                    "lampray.article.publish.duration")
            .description("Measures article publish latency.")
            .histogram()
            .percentiles(0.5D, 0.95D, 0.99D)
            .serviceLevelObjectives(
                    Duration.ofMillis(100),
                    Duration.ofMillis(300),
                    Duration.ofSeconds(1)
            )
            .build();

    private ArticleMetrics() {
    }
}
