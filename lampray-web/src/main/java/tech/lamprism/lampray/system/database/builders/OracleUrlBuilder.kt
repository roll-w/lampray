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

package tech.lamprism.lampray.system.database.builders

import tech.lamprism.lampray.system.database.DatabaseConfig
import tech.lamprism.lampray.system.database.DatabaseType

/**
 * URL builder for Oracle databases.
 * Handles Oracle-specific connection parameters and TNS configurations.
 *
 * @author RollW
 */
class OracleUrlBuilder : AbstractDatabaseUrlBuilder() {

    override val supportedTypes = setOf(DatabaseType.ORACLE)

    override fun buildBaseUrl(config: DatabaseConfig): String {
        val target = config.target

        return if (target.isNetwork()) {
            val service = config.databaseName.ifBlank {
                throw IllegalArgumentException("Database name must be specified for Oracle")
            }
            val protocolPrefix = if (config.ssl.isEnabled()) {
                "jdbc:oracle:thin:@tcps://"
            } else {
                config.type.urlPrefix
            }
            "$protocolPrefix${target.getNetworkAddress()}/$service"
        } else {
            throw IllegalArgumentException("Oracle requires network target format (host:port or host)")
        }
    }

    override fun addCharsetParameter(params: MutableMap<String, String>, charset: String) {
        // Oracle uses oracle.jdbc.defaultNChar parameter for character encoding
        when (charset.lowercase()) {
            "utf8", "utf-8" -> {
                params["oracle.jdbc.defaultNChar"] = "true"
                params["oracle.jdbc.UseNLSProcessing"] = "false"
            }

            "utf8mb4" -> {
                params["oracle.jdbc.defaultNChar"] = "true"
                params["oracle.jdbc.UseNLSProcessing"] = "false"
            }

            else -> {
                params["oracle.jdbc.defaultNChar"] = "false"
            }
        }
    }

    override fun buildSslProperties(config: DatabaseConfig): Map<String, String> {
        if (!config.ssl.isEnabled()) {
            return emptyMap()
        }

        if (config.ssl.hasCustomMaterial()) {
            throw IllegalArgumentException(
                "Oracle managed SSL does not support custom certificate material in this implementation."
            )
        }

        if (config.ssl.mode == tech.lamprism.lampray.system.database.DatabaseSslMode.VERIFY_CA) {
            throw IllegalArgumentException(
                "Oracle does not support a managed verify-ca mode without identity matching. " +
                        "Use 'required', 'verify-identity', or supplemental driver properties in database.options."
            )
        }

        val dnMatch = when (config.ssl.mode) {
            tech.lamprism.lampray.system.database.DatabaseSslMode.DISABLED -> "false"
            tech.lamprism.lampray.system.database.DatabaseSslMode.REQUIRED -> "false"
            tech.lamprism.lampray.system.database.DatabaseSslMode.VERIFY_IDENTITY -> "true"
            tech.lamprism.lampray.system.database.DatabaseSslMode.VERIFY_CA -> "false"
        }

        return mapOf("oracle.net.ssl_server_dn_match" to dnMatch)
    }

    override fun getReservedSslOptionKeys(): Set<String> = setOf("oracle.net.ssl_server_dn_match")

    override fun getDefaultValidationQuery(): String = "SELECT 1 FROM DUAL"

    override fun validateConfig(config: DatabaseConfig) {
        super.validateConfig(config)

        // Oracle-specific validations
        require(config.target.isNetwork()) {
            "Oracle requires network target format (host:port or host), got: ${config.target}"
        }
    }
}
