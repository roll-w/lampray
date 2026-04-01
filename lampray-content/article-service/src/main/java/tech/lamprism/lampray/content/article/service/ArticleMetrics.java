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

import tech.lamprism.lampray.observability.DefaultMetricSpecification;
import tech.lamprism.lampray.observability.MetricSpecification;
import tech.lamprism.lampray.observability.MetricType;

import java.time.Duration;
import java.util.List;

/**
 * @author RollW
 */
public final class ArticleMetrics {
    public static final MetricSpecification ARTICLE_PUBLISH_REQUESTS = DefaultMetricSpecification.builder(
                    "lampray.article.publish.requests",
                    MetricType.COUNTER)
            .description("Counts article publish attempts by result.")
            .allowTags("result")
            .build();

    public static final MetricSpecification ARTICLE_PUBLISH_DURATION = DefaultMetricSpecification.builder(
                    "lampray.article.publish.duration",
                    MetricType.TIMER)
            .description("Measures article publish latency.")
            .histogram()
            .percentiles(0.5D, 0.95D, 0.99D)
            .serviceLevelObjectives(
                    Duration.ofMillis(100).toNanos(),
                    Duration.ofMillis(300).toNanos(),
                    Duration.ofSeconds(1).toNanos()
            )
            .build();

    public static final List<MetricSpecification> SPECIFICATIONS = List.of(
            ARTICLE_PUBLISH_REQUESTS,
            ARTICLE_PUBLISH_DURATION
    );

    private ArticleMetrics() {
    }
}
