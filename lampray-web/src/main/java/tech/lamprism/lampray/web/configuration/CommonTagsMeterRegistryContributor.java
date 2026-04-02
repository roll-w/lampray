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

package tech.lamprism.lampray.web.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.observability.MeterRegistryContributor;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.web.common.keys.ObservabilityConfigKeys;

import java.util.ArrayList;
import java.util.List;

/**
 * @author RollW
 */
@Component
public class CommonTagsMeterRegistryContributor implements MeterRegistryContributor {
    private final ConfigReader configReader;

    public CommonTagsMeterRegistryContributor(ConfigReader configReader) {
        this.configReader = configReader;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void contribute(MeterRegistry meterRegistry) {
        List<String> commonTags = new ArrayList<>();
        appendCommonTag(commonTags, "application", configReader.get(
                ObservabilityConfigKeys.COMMON_TAG_APPLICATION,
                ObservabilityConfigKeys.DEFAULT_APPLICATION_TAG
        ));
        appendCommonTag(commonTags, "instance", configReader.get(
                ObservabilityConfigKeys.COMMON_TAG_INSTANCE,
                ObservabilityConfigKeys.DEFAULT_INSTANCE_TAG
        ));
        appendCommonTag(commonTags, "environment", configReader.get(
                ObservabilityConfigKeys.COMMON_TAG_ENVIRONMENT,
                ObservabilityConfigKeys.DEFAULT_ENVIRONMENT_TAG
        ));
        if (!commonTags.isEmpty()) {
            meterRegistry.config().commonTags(commonTags.toArray(String[]::new));
        }
    }

    private void appendCommonTag(List<String> tags,
                                 String key,
                                 String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        tags.add(key);
        tags.add(value.trim());
    }
}
