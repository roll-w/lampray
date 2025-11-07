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
class ReadonlyConfigProvider(
    private val configReader: ConfigReader
) : ConfigReader by configReader, ConfigProvider {
    override fun set(key: String, value: String?) = SettingSource.NONE

    override fun <T, V> set(spec: SettingSpecification<T, V>, value: T?) = SettingSource.NONE

    override fun <T, V> reset(spec: SettingSpecification<T, V>): SettingSource = SettingSource.NONE

    override fun supports(key: String): Boolean = false

    override val metadata: ConfigReader.Metadata
        get() = configReader.metadata

    val underlying: ConfigReader
        get() = configReader
}