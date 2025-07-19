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

/**
 * @author RollW
 */
class CombinedConfigProvider(
    private val configProviders: List<ConfigProvider>
) : ConfigProvider {
    private val _metadata = ConfigReader.Metadata(
        "Composed Config Provider",
        configProviders.flatMap { it.metadata.paths }
            .distinct()
            .toList()
    )

    override val metadata: ConfigReader.Metadata
        get() = _metadata

    override fun get(key: String): String? {
        for (reader in configProviders) {
            val value = reader[key]
            if (value != null) {
                return value
            }
        }
        return null
    }

    override fun get(key: String, defaultValue: String?): String? {
        return this[key] ?: defaultValue
    }

    override fun <T, V> get(specification: SettingSpecification<T, V>): T? {
        for (reader in configProviders) {
            val value = reader[specification]
            if (value != null) {
                return value
            }
        }
        return null
    }

    override fun <T, V> get(specification: SettingSpecification<T, V>, defaultValue: T): T {
        return this[specification] ?: defaultValue
    }

    override fun <T, V> getValue(specification: SettingSpecification<T, V>): ConfigValue<T, V> {
        for (reader in configProviders) {
            val value = reader.getValue(specification)
            if (value.value != null) {
                return value
            }
        }
        return SnapshotConfigValue(null, SettingSource.NONE, specification)
    }

    override fun list(): List<RawSettingValue> {
        return configProviders.flatMap { it.list() }
    }

    override fun set(key: String, value: String?): SettingSource {
        for (provider in configProviders) {
            if (provider.supports(key)) {
                return provider.set(key, value)
            }
        }
        return SettingSource.NONE
    }

    override fun <T, V> set(spec: SettingSpecification<T, V>, value: T?): SettingSource {
        for (provider in configProviders) {
            if (provider.supports(spec)) {
                return provider.set(spec, value)
            }
        }
        return SettingSource.NONE
    }

    override fun supports(key: String): Boolean {
        return configProviders.any { it.supports(key) }
    }
}