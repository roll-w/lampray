/*
 * Copyright (C) 2023 RollW
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
@Deprecated("Use ConfigValueParser instead")
object SettingSpecificationHelper {
    @Suppress("UNCHECKED_CAST")
    @Deprecated("Use ConfigValueParser instead")
    fun <T> String?.deserialize(specification: SettingSpecification<T>): T? {
        if (this == null) {
            return null
        }
        return when (specification.key.type) {
            ConfigType.STRING -> this as T?
            ConfigType.INT -> this.toIntOrNull() as T?
            ConfigType.LONG -> this.toLongOrNull() as T?
            ConfigType.FLOAT -> this.toFloatOrNull() as T?
            ConfigType.DOUBLE -> this.toDoubleOrNull() as T?
            ConfigType.BOOLEAN -> this.toBoolean() as T?
            // TODO: implement a better way to deserialize set
            ConfigType.STRING_SET -> this.split(",").toSet() as T?
            else -> throw IllegalArgumentException("Unsupported type: $this")
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Deprecated("Use ConfigValueParser instead")
    fun <T> T?.serialize(specification: SettingSpecification<T>): String? {
        if (this == null) {
            return null
        }
        return when (specification.key.type) {
            ConfigType.STRING -> this as String
            ConfigType.INT -> this.toString()
            ConfigType.LONG -> this.toString()
            ConfigType.FLOAT -> this.toString()
            ConfigType.DOUBLE -> this.toString()
            ConfigType.BOOLEAN -> this.toString()
            // TODO: implement a better way to serialize set
            ConfigType.STRING_SET -> (this as Set<String>).joinToString(",")
            else -> throw IllegalArgumentException("Unsupported type: $this")
        }
    }
}