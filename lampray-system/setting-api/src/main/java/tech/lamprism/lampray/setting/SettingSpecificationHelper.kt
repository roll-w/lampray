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
object SettingSpecificationHelper {
    @Suppress("UNCHECKED_CAST")
    fun <T, V> String?.deserialize(specification: SettingSpecification<T, V>): T? {
        if (this == null) {
            return null
        }
        return when (specification.key.type) {
            SettingType.STRING -> this as T?
            SettingType.INT -> this.toIntOrNull() as T?
            SettingType.LONG -> this.toLongOrNull() as T?
            SettingType.FLOAT -> this.toFloatOrNull() as T?
            SettingType.DOUBLE -> this.toDoubleOrNull() as T?
            SettingType.BOOLEAN -> this.toBoolean() as T?
            // TODO: implement a better way to deserialize set
            SettingType.STRING_SET -> this.split(",").toSet() as T?
            else -> throw IllegalArgumentException("Unsupported type: $this")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T, V> T?.serialize(specification: SettingSpecification<T, V>): String? {
        if (this == null) {
            return null
        }
        return when (specification.key.type) {
            SettingType.STRING -> this as String
            SettingType.INT -> this.toString()
            SettingType.LONG -> this.toString()
            SettingType.FLOAT -> this.toString()
            SettingType.DOUBLE -> this.toString()
            SettingType.BOOLEAN -> this.toString()
            // TODO: implement a better way to serialize set
            SettingType.STRING_SET -> (this as Set<String>).joinToString(",")
            else -> throw IllegalArgumentException("Unsupported type: $this")
        }
    }

    fun <T, V> SettingSpecification<T, V>.validate(value: String) {
        try {
            when (key.type) {
                SettingType.INT -> value.toInt()
                SettingType.LONG -> value.toLong()
                SettingType.FLOAT -> value.toFloat()
                SettingType.DOUBLE -> value.toDouble()
                SettingType.BOOLEAN -> value.toBoolean()
                SettingType.STRING, SettingType.STRING_SET -> {}
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid value: $value for '$key', expected type: ${key.type}.")
        }
    }
}