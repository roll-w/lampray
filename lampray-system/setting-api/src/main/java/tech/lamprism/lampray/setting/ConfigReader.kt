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
interface ConfigReader {
    val metadata: Metadata

    /**
     * Recomment use [get] ([SettingSpecification]) instead of this method.
     */
    operator fun get(key: String): String?

    /**
     * Recomment use [get] ([SettingSpecification]) instead of this method.
     */
    operator fun get(key: String, defaultValue: String?): String?

    operator fun <T, V> get(specification: SettingSpecification<T, V>): T?

    operator fun <T, V> get(specification: SettingSpecification<T, V>, defaultValue: T): T

    fun <T, V> getValue(specification: SettingSpecification<T, V>): ConfigValue<T, V>

    /**
     * Get multiple config values by specifications.
     *
     * Note: The order of the returned list is the same as the order of the input specifications.
     */
    fun list(specifications: List<SettingSpecification<*, *>>): List<ConfigValue<*, *>>

    /**
     * Metadata of the config reader.
     */
    data class Metadata(
        /**
         * The name of the config reader.
         */
        val name: String,

        val paths: List<ConfigPath> = emptyList(),
    ) {
        constructor(name: String) : this(name, listOf())

        constructor(name: String, vararg paths: ConfigPath) : this(name, listOf(*paths))

        val settingSources: List<SettingSource>
            get() = paths.map { it.source }
    }
}