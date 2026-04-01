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
import tech.lamprism.lampray.observability.MetricSpecification;
import tech.lamprism.lampray.observability.MetricSpecificationProvider;

import java.util.List;

/**
 * @author RollW
 */
@Component
public final class ArticleMetricSpecifications implements MetricSpecificationProvider {
    @Override
    public List<MetricSpecification> getMetricSpecifications() {
        return ArticleMetrics.SPECIFICATIONS;
    }
}
