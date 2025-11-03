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

import tech.lamprism.lampray.setting.AttributedSettingSpec.Companion.withAttributes

/**
 * @author RollW
 */
class SettingSpecificationBuilder<T, V> {
    constructor()

    constructor(key: SettingKey<T, V>) {
        this.key = key

        if (key == SettingType.BOOLEAN) {
            this.valueEntries = listOf(true, false) as List<V?>
            this.allowAnyValue = false
        }
    }

    var key: SettingKey<T, V>? = null
        private set

    var allowAnyValue: Boolean = true
        private set

    var description: SettingDescription = SettingDescription.EMPTY
        private set

    var supportedSources: List<SettingSource> = SettingSource.LOCAL_ONLY
        private set

    var isRequired: Boolean = false
        private set

    var defaults: List<Int> = emptyList()
        private set

    var valueEntries: List<V?> = emptyList()
        private set

    var secret: Boolean = false
        private set

    fun setKey(key: SettingKey<T, V>) = apply {
        this.key = key
    }

    fun setDescription(description: SettingDescription) = apply {
        this.description = description
    }

    fun setTextDescription(description: String) = apply {
        this.description = SettingDescription.text(description)
    }

    fun setResourceDescription(key: String) = apply {
        this.description = SettingDescription.resource(key)
    }

    fun setAllowAnyValue(allowAnyValue: Boolean) = apply {
        this.allowAnyValue = allowAnyValue
    }

    fun setSupportedSources(supportedSources: List<SettingSource>) = apply {
        this.supportedSources = supportedSources
    }

    fun setRequired(isRequired: Boolean) = apply {
        this.isRequired = isRequired
    }

    fun setDefaults(defaults: List<Int>) = apply {
        this.defaults = defaults
    }

    fun setDefault(default: Int) = apply {
        this.defaults = listOf(default)
    }

    /**
     * Sets the default value, which must be in the value entries. If the value entries
     * is empty, it will be initialized with the default value.
     *
     * Must call after [setValueEntries] if value entries is needed.
     */
    fun setDefaultValue(default: V?) = apply {
        if (valueEntries.isNotEmpty()) {
            // TODO: fix default value setting
            if (default != null && !valueEntries.contains(default)) {
                throw IllegalArgumentException("Default value $default is not in the value entries $valueEntries")
            }
            this.defaults = valueEntries.mapIndexedNotNull { index, value ->
                if (value == default) index else null
            }
            return@apply
        }
        this.defaults = listOf(0)
        this.valueEntries = listOf(default)
    }

    fun setValueEntries(valueEntries: List<V?>) = apply {
        this.valueEntries = valueEntries
    }

    fun setSecret(secret: Boolean) = apply {
        this.secret = secret
    }

    fun buildSimple(): SettingSpecification<T, V> {
        return SimpleSettingSpec(
            key = key!!,
            allowAnyValue = allowAnyValue,
            defaults = defaults,
            valueEntries = valueEntries,
            isRequired = isRequired
        )
    }

    fun build(): AttributedSettingSpecification<T, V> {
        return buildSimple().withAttributes(
            description = description,
            secret = secret,
            supportedSources = supportedSources
        )
    }
}