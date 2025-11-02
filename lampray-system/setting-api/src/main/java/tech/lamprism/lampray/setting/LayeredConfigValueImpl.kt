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

import tech.lamprism.lampray.TimeAttributed
import java.time.OffsetDateTime

/**
 * @author RollW
 */
class LayeredConfigValueImpl<T, V>(
    /**
     * The setting specification that defines the config key and type.
     */
    override val specification: SettingSpecification<T, V>,

    layers: List<ConfigValue<T, V>>
) : LayeredConfigValue<T, V>, TimeAttributed {

    private val _layers = layers.sortedBy { it.source.priority }

    override val layers: List<ConfigValue<T, V>>
        get() = _layers

    init {
        require(layers.isNotEmpty()) { "Layers must be specified." }
    }

    private val activeValue: ConfigValue<T, V> by lazy {
        _layers.lastOrNull { it.value != null } ?: _layers.first()
    }

    override fun getCreateTime(): OffsetDateTime =
        TimeAttributed.NONE_TIME

    override fun getUpdateTime(): OffsetDateTime {
        if (activeValue is TimeAttributed) {
            return (activeValue as TimeAttributed).getUpdateTime()
        }
        return TimeAttributed.NONE_TIME
    }

    override val value: T?
        get() = activeValue.value

    override val source: SettingSource
        get() = activeValue.source

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LayeredConfigValueImpl<*, *>) return false

        if (specification != other.specification) return false
        if (_layers != other._layers) return false
        if (activeValue != other.activeValue) return false

        return true
    }

    override fun hashCode(): Int {
        var result = specification.hashCode()
        result = 31 * result + _layers.hashCode()
        result = 31 * result + activeValue.hashCode()
        return result
    }

    override fun toString(): String {
        return "LayeredConfigValueImpl(" +
                "specification=$specification, " +
                "layers=$layers, " +
                "activeValue=$activeValue" +
                ")"
    }
}