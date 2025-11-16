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

package tech.lamprism.lampray.setting

import org.springframework.cache.Cache
import tech.lamprism.lampray.setting.SettingSpecification.Companion.keyName

/**
 * @author RollW
 */
class CacheSupportConfigProvider(
    private val delegate: ConfigProvider,
    private val cache: Cache
) : ConfigProvider by delegate {
    override val metadata: ConfigReader.Metadata
        get() = delegate.metadata

    private fun specKeyOf(spec: SettingSpecification<*>) = "S${spec.keyName}"

    override fun <T> get(specification: SettingSpecification<T>): T? {
        val valueWrapper = cache.get(specKeyOf(specification))
        return if (valueWrapper != null) {
            (valueWrapper.get() as ConfigValue<T>).value as T?
        } else {
            val configValue = delegate.getValue(specification)
            cache.put(specKeyOf(specification), configValue)
            configValue.value as T?
        }
    }

    override fun <T> get(
        specification: SettingSpecification<T>,
        defaultValue: T
    ): T {
        val valueWrapper = cache.get(specKeyOf(specification))
        return if (valueWrapper != null) {
            (valueWrapper.get() as ConfigValue<T>).value as T
        } else {
            val configValue = delegate.getValue(specification)
            cache.put(specKeyOf(specification), configValue)
            configValue.value as T
        }
    }

    override fun <T> getValue(specification: SettingSpecification<T>): ConfigValue<T> {
        val valueWrapper = cache.get(specKeyOf(specification))
        return if (valueWrapper != null) {
            valueWrapper.get() as ConfigValue<T>
        } else {
            val configValue = delegate.getValue(specification)
            cache.put(specKeyOf(specification), configValue)
            configValue
        }
    }

    override fun <T> set(spec: SettingSpecification<T>, value: T?): SettingSource {
        return delegate.set(spec, value).also { source ->
            if (source == SettingSource.NONE) {
                return@also
            }
            // If set is successful, evict the cache entry. The new value will be cached on next get.
            // Why evict instead of put? Because the conversion from T to ConfigValue<T> may be complex,
            // and it's better to let the child ConfigProvider handle it.
            cache.evictIfPresent(specKeyOf(spec))
        }
    }

    override fun <T> set(configValue: ConfigValue<T>): SettingSource {
        return delegate.set(configValue).also { source ->
            if (source == SettingSource.NONE) {
                return@also
            }
            cache.evictIfPresent(specKeyOf(configValue.specification))
        }
    }

    override fun <T> reset(spec: SettingSpecification<T>): SettingSource {
        return delegate.reset(spec).also { source ->
            if (source == SettingSource.NONE) {
                return@also
            }
            cache.evictIfPresent(specKeyOf(spec))
        }
    }
}