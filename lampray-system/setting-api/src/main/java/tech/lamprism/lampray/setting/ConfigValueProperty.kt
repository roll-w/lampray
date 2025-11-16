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

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A delegate for config value.
 * Can be used to read and write config value.
 *
 * @author RollW
 */
class ConfigValueProperty<T>(
    override val specification: AttributedSettingSpecification<T>,
    override val source: SettingSource,
    private val reader: ConfigReader,
    private val writer: ConfigWriter? = null,
    /**
     * If true, the value can be any value, otherwise,
     * the value must be one of the allowed values in
     * [SettingSpecification.valueEntries]
     */
    private val allowAnyValue: Boolean = specification.allowAnyValue()
) : ReadWriteProperty<Any?, T?>, ConfigValue<T> {

    override fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): T? {
        return getConfigValue()
    }

    override fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: T?
    ) {
        setConfigValue(value)
    }

    override var value: T?
        get() = getConfigValue()
        set(value) {
            setConfigValue(value)
        }

    private fun getConfigValue(): T? {
        return reader[specification]
    }

    private fun setConfigValue(value: T?) {
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
    private fun SettingSpecification<T>.hasValueByType(value: T?): Boolean {
        // TODO
//        return if (key.type == ConfigType.STRING_SET) {
//            valueEntries.containsAll(value as Set<*>)
//        } else {
//            valueEntries.contains(value as T)
//        }
        return true
    }

    companion object {
        fun <T> AttributedSettingSpecification<T>.value(
            configProvider: ConfigProvider,
            settingSource: SettingSource,
            allowAnyValue: Boolean = this.allowAnyValue()
        ): ConfigValueProperty<T> {
            return ConfigValueProperty(
                this, settingSource,
                configProvider,
                allowAnyValue = allowAnyValue
            )
        }

        fun ConfigProvider.configValue(
            specification: AttributedSettingSpecification<*>,
            settingSource: SettingSource,
            allowAnyValue: Boolean = specification.allowAnyValue()
        ): ConfigValueProperty<*> {
            return ConfigValueProperty(
                specification,
                settingSource, this,
                allowAnyValue = allowAnyValue
            )
        }
    }
}