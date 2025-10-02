/*
 * Copyright (C) 2025 RollW
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

import tech.lamprism.lampray.TimeAttributed
import tech.lamprism.lampray.setting.SettingSpecification.Companion.keyName
import java.time.OffsetDateTime

/**
 * Config information including metadata and value details.
 *
 * @author RollW
 */
data class ConfigValueInfo<T, V>(
    /**
     * The setting specification that defines the config key and type.
     */
    override val specification: SettingSpecification<T, V>,

    /**
     * The raw key string.
     */
    val key: String,

    /**
     * The current value of the config.
     */
    override val value: T?,

    /**
     * The raw value as stored in the source in its string form.
     */
    val rawValue: String?,

    /**
     * The source where this config value comes from.
     */
    override val source: SettingSource,

    /**
     * The last modified time of this config if available.
     */
    val lastModified: OffsetDateTime?
) : ConfigValue<T, V>, TimeAttributed {
    override fun getCreateTime(): OffsetDateTime =
        TimeAttributed.NONE_TIME

    override fun getUpdateTime(): OffsetDateTime =
        lastModified ?: TimeAttributed.NONE_TIME

    companion object {
        @JvmStatic
        fun <T, V> from(
            specification: SettingSpecification<T, V>,
            value: T?,
            rawValue: String?,
            source: SettingSource,
            lastModified: OffsetDateTime?
        ): ConfigValueInfo<T, V> {
            return ConfigValueInfo(
                specification = specification,
                key = specification.keyName,
                value = value,
                rawValue = rawValue,
                source = source,
                lastModified = lastModified
            )
        }
    }
}
