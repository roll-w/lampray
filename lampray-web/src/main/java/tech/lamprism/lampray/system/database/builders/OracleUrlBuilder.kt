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

import tech.lamprism.lampray.system.database.CertificateValue
import tech.lamprism.lampray.system.database.DatabaseConfig
import tech.lamprism.lampray.system.database.DatabaseType
import tech.lamprism.lampray.system.database.SslConfig

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
            "${config.type.urlPrefix}${target.getNetworkAddress()}/$service"
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

    override fun addSslParameters(params: MutableMap<String, String>, config: DatabaseConfig) {
        when (config.sslConfig.mode) {
            SslConfig.Mode.DISABLE -> {
                // Oracle uses non-SSL connection by default
            }

            SslConfig.Mode.PREFER, SslConfig.Mode.REQUIRE -> {
                params["oracle.jdbc.useSSL"] = "true"
            }

            SslConfig.Mode.VERIFY_CA, SslConfig.Mode.VERIFY_IDENTITY -> {
                params["oracle.jdbc.useSSL"] = "true"
                params["oracle.net.ssl_server_dn_match"] = "true"
            }
        }

        // Handle certificate configuration
        config.sslConfig.caCertificate?.let {
            if (!it.isEmpty() && it.type == CertificateValue.CertificateType.PATH) {
                params["oracle.net.ssl_server_cert_path"] = it.value
            }
        }
    }

    override fun getDefaultValidationQuery(): String = "SELECT 1 FROM DUAL"

    override fun validateConfig(config: DatabaseConfig) {
        super.validateConfig(config)

        // Oracle-specific validations
        require(config.target.isNetwork()) {
            "Oracle requires network target format (host:port or host), got: ${config.target}"
        }
    }
}