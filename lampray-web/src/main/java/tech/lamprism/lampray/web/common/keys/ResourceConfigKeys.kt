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

package tech.lamprism.lampray.web.common.keys

import org.springframework.stereotype.Component
import tech.lamprism.lampray.setting.AttributedSettingSpecification
import tech.lamprism.lampray.setting.SettingKey
import tech.lamprism.lampray.setting.SettingSource
import tech.lamprism.lampray.setting.SettingSpecificationBuilder
import tech.lamprism.lampray.setting.SettingSpecificationSupplier

/**
 * @author RollW
 */
@Component
object ResourceConfigKeys : SettingSpecificationSupplier {
    const val EMBEDDED_RESOURCE = "[embedded]"

    // TODO: may rename setting keys
    @JvmField
    val FRONTEND_ENABLED =
        SettingSpecificationBuilder(SettingKey.ofBoolean("resource.frontend.enabled"))
            .setTextDescription("""
                Enable frontend resource hosting.
                """.trimIndent())
            .setValueEntries(listOf(false, true))
            .setDefaultValue(false)
            .setSupportedSources(SettingSource.VALUES)
            .setRequired(false)
            .build()

    @JvmField
    val RESOURCE_SOURCE =
        SettingSpecificationBuilder(SettingKey.ofString("resource.frontend.path"))
            .setTextDescription("""
                Frontend resource path, must be local path or embedded. 
                
                Note: If self-hosting is not enabled, this configuration will be ignored.
                """.trimIndent())
            .setDefaultValue(EMBEDDED_RESOURCE)
            .setSupportedSources(SettingSource.VALUES)
            .setRequired(false)
            .build()

    private val keys = listOf(
        FRONTEND_ENABLED, RESOURCE_SOURCE
    )

    override val specifications: List<AttributedSettingSpecification<*>>
        get() = keys
}