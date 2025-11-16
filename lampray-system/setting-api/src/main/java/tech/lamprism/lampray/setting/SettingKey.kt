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
 * The key of a setting.
 *
 * @see ConfigType
 * @author RollW
 */
data class SettingKey<T>(
    val name: String,
    val type: ConfigType<T>,
) {
    init {
        require(name.isNotEmpty()) { "Name must not be empty" }
    }

    companion object {
        @JvmStatic
        fun <T> of(name: String, type: ConfigType<T>): SettingKey<T> {
            return SettingKey(name, type)
        }

        @JvmStatic
        fun ofString(name: String): SettingKey<String> {
            return of(name, ConfigType.STRING)
        }

        @JvmStatic
        fun ofStringSet(name: String): SettingKey<Set<String>> {
            return of(name, ConfigType.STRING_SET)
        }

        @JvmStatic
        fun ofInt(name: String): SettingKey<Int> {
            return of(name, ConfigType.INT)
        }

        @JvmStatic
        fun ofLong(name: String): SettingKey<Long> {
            return of(name, ConfigType.LONG)
        }

        @JvmStatic
        fun ofFloat(name: String): SettingKey<Float> {
            return of(name, ConfigType.FLOAT)
        }

        @JvmStatic
        fun ofDouble(name: String): SettingKey<Double> {
            return of(name, ConfigType.DOUBLE)
        }

        @JvmStatic
        fun ofBoolean(name: String): SettingKey<Boolean> {
            return of(name, ConfigType.BOOLEAN)
        }
    }
}
