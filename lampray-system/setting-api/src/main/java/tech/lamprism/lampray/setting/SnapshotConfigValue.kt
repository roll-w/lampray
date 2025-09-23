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

/**
 * A read-only, snapshot of a [ConfigValue].
 *
 * @author RollW
 */
data class SnapshotConfigValue<T, V>(
    override val value: T?,
    override val source: SettingSource,
    override val specification: SettingSpecification<T, V>
) : ConfigValue<T, V>, SettingSpecification<T, V> by specification {
    override fun toString(): String {
        return "SnapshotConfigValue[${specification.keyName}](value=$value, source=$source)"
    }
}