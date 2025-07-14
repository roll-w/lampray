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

import tech.lamprism.lampray.setting.AttributedSettingSpecification
import tech.lamprism.lampray.setting.SettingKey
import tech.lamprism.lampray.setting.SettingSource
import tech.lamprism.lampray.setting.SettingSpecificationBuilder
import tech.lamprism.lampray.setting.SettingSpecificationSupplier

/**
 * @author RollW
 */
object ServerConfigKeys : SettingSpecificationSupplier {

    @JvmField
    val HTTP_PORT =
        SettingSpecificationBuilder(SettingKey.ofInt("server.http.port"))
            .setTextDescription("HTTP server port")
            .setDefaultValue(5100)
            .setSupportedSources(SettingSource.LOCAL_ONLY)
            .setRequired(true)
            .build()

    @JvmField
    val HTTP_HOST =
        SettingSpecificationBuilder(SettingKey.ofString("server.http.host"))
            .setResourceDescription("HTTP server host")
            .setDefaultValue(null)
            .setSupportedSources(SettingSource.LOCAL_ONLY)
            .setRequired(false)
            .build()

    @JvmField
    val PROCESS_PROXY_HEADERS =
        SettingSpecificationBuilder(SettingKey.ofBoolean("server.http.proxy.headers"))
            .setTextDescription("Process proxy headers for HTTP server, if enabled, the server will " +
                    "process the Forwarded, X-Forwarded-For and X-Forwarded-Proto and other headers " +
                    "to determine the original request information, such as the original IP address and protocol.")
            .setDefaultValue(false)
            .setSupportedSources(SettingSource.LOCAL_ONLY)
            .setRequired(false)
            .build()

    @JvmField
    val SSH_PORT =
        SettingSpecificationBuilder(SettingKey.ofInt("server.ssh.port"))
            .setTextDescription("SSH server port")
            .setDefaultValue(5101)
            .setSupportedSources(SettingSource.LOCAL_ONLY)
            .setRequired(true)
            .build()

    @JvmField
    val SSH_HOST =
        SettingSpecificationBuilder(SettingKey.ofString("server.ssh.host"))
            .setTextDescription("SSH server host")
            .setDefaultValue(null)
            .setSupportedSources(SettingSource.LOCAL_ONLY)
            .setRequired(false)
            .build()

    @JvmField
    val SSH_HOST_KEY =
        SettingSpecificationBuilder(SettingKey.ofString("server.ssh.host.key"))
            .setTextDescription("Private key path for SSH host, if specified file does not exist, " +
                    "a new key will be generated")
            .setDefaultValue("conf/ssh_host.key")
            .setSupportedSources(SettingSource.LOCAL_ONLY)
            .setRequired(false)
            .build()

    private val keys = listOf(
        HTTP_PORT, HTTP_HOST, PROCESS_PROXY_HEADERS, SSH_PORT, SSH_HOST, SSH_HOST_KEY
    )

    override val specifications: List<AttributedSettingSpecification<*, *>>
        get() = keys
}