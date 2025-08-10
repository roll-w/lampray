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
package tech.lamprism.lampray.setting.event

import org.springframework.context.ApplicationEventPublisher
import tech.lamprism.lampray.setting.AttributedSettingSpecification
import tech.lamprism.lampray.setting.ConfigProvider
import tech.lamprism.lampray.setting.ConfigValue
import tech.lamprism.lampray.setting.SettingSource
import tech.lamprism.lampray.setting.SettingSpecification
import tech.lamprism.lampray.setting.SettingSpecificationHelper.deserialize
import tech.lamprism.lampray.setting.SettingSpecificationProvider

/**
 * @author RollW
 */
class EventProxyConfigProvider(
    private val configProvider: ConfigProvider,
    private val specificationProvider: SettingSpecificationProvider,
    private val applicationEventPublisher: ApplicationEventPublisher
) : ConfigProvider by configProvider {

    override fun <T, V> set(spec: SettingSpecification<T, V>, value: T?): SettingSource {
        return configProvider.set(spec, value).also {
            if (it == SettingSource.NONE) {
                return@also
            }
            applicationEventPublisher.publishEvent(
                SettingValueChangedEvent(spec, value)
            )
        }
    }

    override fun <T, V> set(configValue: ConfigValue<T, V>): SettingSource {
        return configProvider.set(configValue).also {
            if (it == SettingSource.NONE) {
                return@also
            }
            applicationEventPublisher.publishEvent(
                SettingValueChangedEvent(configValue.specification, configValue.value)
            )
        }
    }

    override fun set(key: String, value: String?): SettingSource {
        return configProvider.set(key, value).also {
            if (it == SettingSource.NONE) {
                return@also
            }
            publishEvent<Any, Any>(key, value)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T, V> publishEvent(key: String, value: String?) = try {
        val spec: AttributedSettingSpecification<T, V> = specificationProvider
            .getSettingSpecification(key) as AttributedSettingSpecification<T, V>
        val event =
            SettingValueChangedEvent(spec, value.deserialize(spec))
        applicationEventPublisher.publishEvent(event)
    } catch (_: Exception) {
        // ignore
    }
}
