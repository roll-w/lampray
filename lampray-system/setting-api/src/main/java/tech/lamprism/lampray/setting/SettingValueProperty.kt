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

import tech.lamprism.lampray.setting.SettingSpecification.Companion.keyName
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A delegate for setting value. Can be used to read
 * and write setting value.
 *
 * @author RollW
 */
class SettingValueProperty<T, V>(
    override val specification: AttributedSettingSpecification<T, V>,
    override val source: SettingSource,
    private val reader: ConfigReader,
    private val writer: ConfigWriter? = null,
    /**
     * If true, the value can be any value, otherwise,
     * the value must be one of the allowed values in
     * [SettingSpecification.valueEntries]
     */
    private val allowAnyValue: Boolean = specification.allowAnyValue()
) : ReadWriteProperty<Any?, T?>, ConfigValue<T, V> {

    init {
        require(!specification.isTemplate()) {
            "SettingValueProperty cannot be created from a template specification: ${specification.keyName}"
        }
    }

    override fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): T? {
        return getSettingValue()
    }

    override fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: T?
    ) {
        setSettingValue(value)
    }

    override var value: T?
        get() = getSettingValue()
        set(value) {
            setSettingValue(value)
        }

    private fun getSettingValue(): T? {
        return reader[specification]
    }

    private fun setSettingValue(value: T?) {
        checkValueIn(value)
        writer?.let {
            it[specification] = value
        }
    }

    private fun checkValueIn(value: T?) {
        if (allowAnyValue || value == null) {
            return
        }
        if (!specification.hasValueByType(value)) {
            throw IllegalArgumentException(
                "Value $value is not allowed by key '${specification.key.name}'"
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun SettingSpecification<T, V>.hasValueByType(value: T?): Boolean {
        return if (key.type == SettingType.STRING_SET) {
            valueEntries.containsAll(value as Set<*>)
        } else {
            valueEntries.contains(value as V)
        }
    }

    companion object {
        fun <T, V> AttributedSettingSpecification<T, V>.value(
            configProvider: ConfigProvider,
            settingSource: SettingSource,
            allowAnyValue: Boolean = this.allowAnyValue()
        ): SettingValueProperty<T, V> {
            return SettingValueProperty(
                this, settingSource,
                configProvider,
                allowAnyValue = allowAnyValue
            )
        }

        fun ConfigProvider.settingValue(
            specification: AttributedSettingSpecification<*, *>,
            settingSource: SettingSource,
            allowAnyValue: Boolean = specification.allowAnyValue()
        ): SettingValueProperty<*, *> {
            return SettingValueProperty(
                specification,
                settingSource, this,
                allowAnyValue = allowAnyValue
            )
        }
    }
}