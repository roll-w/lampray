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
data class AttributedSettingSpec<T, V> @JvmOverloads constructor(
    private val specification: SettingSpecification<T, V>,
    override val description: SettingDescription = SettingDescription.EMPTY,
    override val secret: Boolean = false,
    override val supportedSources: List<SettingSource> = SettingSource.LOCAL_ONLY
) : SettingSpecification<T, V> by specification, AttributedSettingSpecification<T, V> {

    companion object {
        @JvmStatic
        fun <T, V> SettingSpecification<T, V>.withAttributes(
            description: SettingDescription = SettingDescription.EMPTY,
            secret: Boolean = false,
            supportedSources: List<SettingSource> = SettingSource.LOCAL_ONLY
        ): AttributedSettingSpec<T, V> {
            return AttributedSettingSpec(
                this,
                description,
                secret,
                supportedSources
            )
        }
    }
}
