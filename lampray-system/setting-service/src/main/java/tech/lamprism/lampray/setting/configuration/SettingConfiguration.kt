/*
 * Copyright (C) 2023-2025 RollW
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

package tech.lamprism.lampray.setting.configuration

import org.springframework.cache.CacheManager
import org.springframework.cache.get
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import tech.lamprism.lampray.setting.CacheSupportConfigProvider
import tech.lamprism.lampray.setting.CombinedConfigProvider
import tech.lamprism.lampray.setting.ConfigProvider
import tech.lamprism.lampray.setting.EnvironmentConfigReader
import tech.lamprism.lampray.setting.MessageSourceSettingDescriptionProvider
import tech.lamprism.lampray.setting.ReadonlyConfigProvider
import tech.lamprism.lampray.setting.SettingSpecificationProvider
import tech.lamprism.lampray.setting.event.EventProxyConfigProvider

/**
 * @author RollW
 */
@Configuration
class SettingConfiguration {

    @Bean
    @Primary
    fun configProvider(
        configProviders: List<ConfigProvider>,
        specificationProvider: SettingSpecificationProvider,
        applicationEventPublisher: ApplicationEventPublisher,
        cacheManager: CacheManager
    ): ConfigProvider {
        return CacheSupportConfigProvider(
            EventProxyConfigProvider(
                CombinedConfigProvider(
                    configProviders.sortedByDescending { it ->
                        it.metadata.settingSources.minOfOrNull { it.ordinal }
                    }
                ),
                specificationProvider,
                applicationEventPublisher
            ), cacheManager["config-cache"]!!
        )
    }

    @Bean
    fun messageSourceSettingDescriptionProvider(
        messageSource: MessageSource
    ) = MessageSourceSettingDescriptionProvider(messageSource)

    @Bean
    fun environmentConfigReader(): ConfigProvider {
        return ReadonlyConfigProvider(EnvironmentConfigReader())
    }
}