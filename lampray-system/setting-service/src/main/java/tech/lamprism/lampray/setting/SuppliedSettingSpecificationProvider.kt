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

import tech.lamprism.lampray.setting.SettingSpecification.Companion.keyName

/**
 * @author RollW
 */
class SuppliedSettingSpecificationProvider(
    private val suppliers: List<SettingSpecificationSupplier>,
    private val overrideDuplicates: Boolean = false
) : SettingSpecificationProvider {
    private val specs: MutableMap<String, AttributedSettingSpecification<*>> = hashMapOf()

    init {
        suppliers.forEach { supplier ->
            supplier.specifications.forEach { spec ->
                if (specs.containsKey(spec.keyName) && !overrideDuplicates) {
                    throw IllegalArgumentException("Duplicate setting key: ${spec.keyName}")
                }
                specs[spec.keyName] = spec
            }
        }
    }

    override fun getSettingSpecification(key: String): AttributedSettingSpecification<*> {
        return specs[key] ?: throw IllegalArgumentException("No such setting: $key")
    }

    override val settingSpecifications: List<AttributedSettingSpecification<*>>
        get() = specs.values.toList()
}