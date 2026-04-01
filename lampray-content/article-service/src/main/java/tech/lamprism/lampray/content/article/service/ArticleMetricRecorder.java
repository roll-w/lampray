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

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.observability.MetricProvider;

import java.time.Duration;
import java.util.Map;

/**
 * @author RollW
 */
@Component
public final class ArticleMetricRecorder {
    private final MetricProvider metricProvider;

    public ArticleMetricRecorder(MetricProvider metricProvider) {
        this.metricProvider = metricProvider;
    }

    public void recordPublishSuccess(Duration duration) {
        metricProvider.recordDuration(ArticleMetrics.ARTICLE_PUBLISH_DURATION, Map.of(), duration);
        metricProvider.increment(ArticleMetrics.ARTICLE_PUBLISH_REQUESTS, publishTags("success"));
    }

    public void recordPublishFailure(String result) {
        metricProvider.increment(ArticleMetrics.ARTICLE_PUBLISH_REQUESTS, publishTags(result));
    }

    private Map<String, String> publishTags(String result) {
        return Map.of("result", result);
    }
}
