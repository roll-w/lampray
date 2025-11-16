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

import com.google.common.base.CaseFormat
import tech.lamprism.lampray.setting.SettingSpecification.Companion.keyName

/**
 * @author RollW
 */
class EnvironmentConfigReader(
    private val filteredKeys: List<String> = emptyList(),
) : ConfigReader {
    override val metadata: ConfigReader.Metadata =
        ConfigReader.Metadata(
            name = "EnvironmentConfigReader",
            paths = listOf(ConfigPath("Environment", SettingSource.ENVIRONMENT))
        )

    private fun get(key: String): String? {
        if (filteredKeys.isNotEmpty() && key !in filteredKeys) {
            return null
        }
        val envKey = key.asEnvKey()
        return System.getenv(envKey)
    }

    override fun <T> get(specification: SettingSpecification<T>): T? {
        return getRaw(specification)
    }

    override fun <T> get(specification: SettingSpecification<T>, defaultValue: T): T {
        return this[specification] ?: defaultValue
    }

    override fun <T> getValue(specification: SettingSpecification<T>): ConfigValue<T> {
        return SnapshotConfigValue(
            get(specification),
            SettingSource.ENVIRONMENT,
            specification
        )
    }

    private fun <T> getRaw(specification: SettingSpecification<T>): T? {
        if (filteredKeys.isNotEmpty() && specification.keyName !in filteredKeys) {
            return null
        }
        val value = get(specification.keyName.asEnvKey()) ?: return null
        return with(SettingSpecificationHelper) {
            value.deserialize(specification)
        }
    }

    override fun list(specifications: List<SettingSpecification<*>>): List<ConfigValue<*>> {
        return specifications.map { getValue(it) }
    }

    private fun String.asEnvKey(): String {
        if (this.isEmpty()) {
            return this
        }
        val underscore = this.replace('.', '_')
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_UNDERSCORE, underscore)
    }
}