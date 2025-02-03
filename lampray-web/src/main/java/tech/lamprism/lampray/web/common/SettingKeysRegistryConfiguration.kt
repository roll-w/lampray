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
package tech.lamprism.lampray.web.common

import org.slf4j.debug
import org.slf4j.logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tech.lamprism.lampray.authentication.SecurityConfigKeys
import tech.lamprism.lampray.push.mail.MailConfigKeys
import tech.lamprism.lampray.setting.SettingSpecification.Companion.keyName
import tech.lamprism.lampray.setting.SettingSpecificationProvider
import tech.lamprism.lampray.setting.SettingSpecificationSupplier
import tech.lamprism.lampray.setting.SuppliedSettingSpecificationProvider
import tech.lamprism.lampray.web.common.keys.DatabaseConfigKeys
import tech.lamprism.lampray.web.common.keys.ServerConfigKeys
import tech.lamprism.lampray.web.common.keys.SystemConfigKeys
import java.lang.management.ManagementFactory

private val logger = logger<SettingKeysRegistryConfiguration>()

/**
 * @author RollW
 */
@Configuration
class SettingKeysRegistryConfiguration {

    @Bean
    fun settingSpecificationProvider(
        settingSpecificationSuppliers: List<SettingSpecificationSupplier>
    ): SettingSpecificationProvider {
        printProps()
        return SuppliedSettingSpecificationProvider(
            listOf(
                SystemConfigKeys,
                ServerConfigKeys,
                DatabaseConfigKeys,
                SecurityConfigKeys.INSTANCE,
                MailConfigKeys.INSTANCE
            ) + settingSpecificationSuppliers
        ).also {
            if (!logger.isDebugEnabled) return it
            it.printSettingKeys()
        }
    }

    /**
     * Print registered setting keys.
     *
     * This is useful for debugging and troubleshooting.
     */
    private fun SettingSpecificationProvider.printSettingKeys() = settingSpecifications
        .sortedBy { it.keyName }.joinToString(separator = "\n") {
            val mark = if (it.isRequired) "*" else ""
            val sources = it.supportedSources.joinToString(",") { it.name }
            " [$sources] (${it.key.type}) ${mark}${it.keyName}=${it.defaultValue}"
        }.let {
            logger.debug { "Registered setting keys:\n$it" }
        }


    /**
     * Print JVM arguments and system properties.
     *
     * This is useful for debugging and troubleshooting.
     */
    private fun printProps() {
        if (!logger.isDebugEnabled) return

        val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
        val arguments = runtimeMxBean.inputArguments
        logger.debug { "JVM arguments:\n${arguments.joinToString("\n", prefix = "  ")}" }

        System.getProperties()
            .map { (k, v) ->
                if (v.toString().length > 100) "  $k=${v.toString().substring(0, 100)}..."
                else "  $k=$v"
            }
            .joinToString("\n").let {
                logger.debug { "System properties: \n$it" }
            }
    }
}
