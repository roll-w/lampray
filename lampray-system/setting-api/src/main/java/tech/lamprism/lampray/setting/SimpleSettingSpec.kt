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
data class SimpleSettingSpec<T, V> @JvmOverloads constructor(
    override val key: SettingKey<T, V>,
    private val allowAnyValue: Boolean = false,
    override val defaults: List<Int> = emptyList(),
    override val valueEntries: List<V?> = emptyList(),
    override val isRequired: Boolean = false
) : SettingSpecification<T, V> {
    val type = key.type

    @JvmOverloads
    constructor(
        key: SettingKey<T, V>,
        isRequired: Boolean = false,
        default: Int,
        vararg valueEntries: V?
    ) : this(
        key,
        defaults = listOf(default),
        valueEntries = valueEntries.toList(),
        isRequired = isRequired
    )

    @JvmOverloads
    constructor(
        key: SettingKey<T, V>,
        default: V?,
        isRequired: Boolean = false
    ) : this(
        key,
        allowAnyValue = true,
        defaults = listOf(0),
        valueEntries = listOf(default),
        isRequired = isRequired
    )

    constructor(
        key: SettingKey<T, V>,
        default: Int,
        allowAnyValue: Boolean,
        vararg valueEntries: V
    ) : this(key, allowAnyValue, listOf(default), valueEntries.toList())

    init {
        checkDefaults()
    }

    private fun checkDefaults() {
        if (valueEntries.isEmpty() || defaults.isEmpty()) {
            return
        }
        if (defaults.size > 1 && type != SettingType.STRING_SET) {
            throw IllegalArgumentException("Only STRING_SET type can have multiple defaults")
        }
        if (defaults.any { it >= valueEntries.size }) {
            throw IllegalArgumentException("Invalid default index: $defaults for ${valueEntries.size}")
        }
    }

    override fun withParameters(parameters: Map<String, String>): SimpleSettingSpec<T, V> {
        val newKey = key.withParameters(parameters)
        // We only need to check the memory address of the key, no need for equals comparison
        // since the key will not change if it has no parameters.
        if (newKey === key) {
            return this
        }
        return copy(
            key = newKey,
            allowAnyValue = allowAnyValue,
            defaults = defaults,
            valueEntries = valueEntries,
            isRequired = isRequired
        )
    }

    override fun withParameters(vararg parameters: Pair<String, String>): SimpleSettingSpec<T, V> {
        return withParameters(parameters.toMap())
    }

    override fun withParameter(
        name: String,
        value: String
    ): SimpleSettingSpec<T, V> {
        return withParameters(mapOf(name to value))
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
            return if (key.type == SettingType.STRING_SET) {
                defaults.map { valueEntries.elementAtOrNull(it) }.toSet() as T?
            } else {
                defaults.map { valueEntries[it] }.firstOrNull() as T?
            }
        }

    override operator fun get(index: Int): V? {
        if (index >= valueEntries.size) {
            return null
        }
        return valueEntries[index]
    }

    override fun hasValue(value: V?): Boolean {
        return valueEntries.contains(value)
    }

    override fun allowAnyValue() = valueEntries.isEmpty() || allowAnyValue

    companion object {
        @JvmOverloads
        @JvmStatic
        fun ofStringSet(
            key: String,
            isRequired: Boolean = false,
            default: List<Int> = emptyList(),
            vararg valueEntries: String
        ) = SimpleSettingSpec(
            SettingKey.ofStringSet(key),
            defaults = default,
            valueEntries = valueEntries.toList()
        )

        @JvmStatic
        @JvmOverloads
        fun ofBoolean(
            key: String,
            isRequired: Boolean = false,
            default: Boolean = false
        ) = SimpleSettingSpec(
            SettingKey.ofBoolean(key),
            default = if (default) 0 else 1,
            valueEntries = arrayOf(true, false)
        )
    }
}
