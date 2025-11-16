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
data class SimpleSettingSpec<T> @JvmOverloads constructor(
    override val key: SettingKey<T>,
    private val allowAnyValue: Boolean = false,
    override val defaults: List<Int> = emptyList(),
    override val valueEntries: List<T?> = emptyList(),
    override val isRequired: Boolean = false,
    override val description: SettingDescription = SettingDescription.EMPTY,
    override val secret: Boolean = false,
    override val supportedSources: List<SettingSource> = SettingSource.LOCAL_ONLY
) : SettingSpecification<T> {
    val type = key.type

    @JvmOverloads
    constructor(
        key: SettingKey<T>,
        isRequired: Boolean = false,
        default: Int,
        vararg valueEntries: T?
    ) : this(
        key,
        defaults = listOf(default),
        valueEntries = valueEntries.toList(),
        isRequired = isRequired
    )

    @JvmOverloads
    constructor(
        key: SettingKey<T>,
        default: T?,
        isRequired: Boolean = false
    ) : this(
        key,
        allowAnyValue = true,
        defaults = listOf(0),
        valueEntries = listOf(default),
        isRequired = isRequired
    )

    constructor(
        key: SettingKey<T>,
        default: Int,
        allowAnyValue: Boolean,
        vararg valueEntries: T
    ) : this(key, allowAnyValue, listOf(default), valueEntries.toList())

    init {
        checkDefaults()
    }

    private fun checkDefaults() {
//        if (valueEntries.isEmpty() || defaults.isEmpty()) {
//            return
//        }
//        if (defaults.size > 1 && type != ConfigType.STRING_SET) {
//            throw IllegalArgumentException("Only STRING_SET type can have multiple defaults")
//        }
//        if (defaults.any { it >= valueEntries.size }) {
//            throw IllegalArgumentException("Invalid default index: $defaults for ${valueEntries.size}")
//        }
    }

    /**
     * The default value of the setting.
     */
    override val defaultValue: T?
        get() {
            if (defaults.isEmpty() || valueEntries.isEmpty()) {
                return null
            }

            @Suppress("UNCHECKED_CAST")
            return if (key.type == ConfigType.STRING_SET) {
                defaults.map { valueEntries.elementAtOrNull(it) }.toSet() as T?
            } else {
                defaults.map { valueEntries[it] }.firstOrNull() as T?
            }
        }

    override operator fun get(index: Int): T? {
        if (index >= valueEntries.size) {
            return null
        }
        return valueEntries[index]
    }

    override fun hasValue(value: T?): Boolean {
        return valueEntries.contains(value)
    }

    override fun allowAnyValue() = valueEntries.isEmpty() || allowAnyValue
}
