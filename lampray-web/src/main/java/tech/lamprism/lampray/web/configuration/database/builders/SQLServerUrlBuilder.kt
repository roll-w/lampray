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

package tech.lamprism.lampray.web.configuration.database.builders

import tech.lamprism.lampray.web.configuration.database.DatabaseConfig
import tech.lamprism.lampray.web.configuration.database.DatabaseType
import tech.lamprism.lampray.web.configuration.database.SslConfig

/**
 * URL builder for SQL Server databases.
 * Handles Microsoft SQL Server connection parameters and authentication.
 *
 * @author RollW
 */
class SQLServerUrlBuilder : AbstractDatabaseUrlBuilder() {

    override val supportedTypes = setOf(DatabaseType.SQL_SERVER)

    override fun buildBaseUrl(config: DatabaseConfig): String {
        val target = config.target

        return if (target.isNetwork()) {
            val database = config.databaseName.ifBlank {
                throw IllegalArgumentException("Database name must be specified for SQL Server")
            }
            "${config.type.urlPrefix}${target.getNetworkAddress()};databaseName=$database"
        } else {
            throw IllegalArgumentException("SQL Server requires network target format (host:port or host)")
        }
    }

    override fun addCharsetParameter(params: MutableMap<String, String>, charset: String) {
        // SQL Server uses characterEncoding parameter
        when (charset.lowercase()) {
            "utf8", "utf-8", "utf8mb4" -> params["characterEncoding"] = "UTF-8"
            else -> params["characterEncoding"] = charset
        }
    }

    override fun addSslParameters(params: MutableMap<String, String>, config: DatabaseConfig) {
        when (config.sslConfig.mode) {
            SslConfig.Mode.DISABLE -> {
                params["encrypt"] = "false"
            }

            SslConfig.Mode.PREFER -> {
                params["encrypt"] = "true"
                params["trustServerCertificate"] = "true"
            }

            SslConfig.Mode.REQUIRE -> {
                params["encrypt"] = "true"
                params["trustServerCertificate"] = "false"
            }

            SslConfig.Mode.VERIFY_CA, SslConfig.Mode.VERIFY_IDENTITY -> {
                params["encrypt"] = "true"
                params["trustServerCertificate"] = "false"
                params["hostNameInCertificate"] = "*.database.windows.net"
            }
        }

        // Handle self-signed certificates
        if (config.sslConfig.allowSelfSignedCertificates) {
            params["trustServerCertificate"] = "true"
        }
    }

    override fun getDefaultValidationQuery(): String = "SELECT 1"

    override fun validateConfig(config: DatabaseConfig) {
        super.validateConfig(config)

        // SQL Server-specific validations
        require(config.target.isNetwork()) {
            "SQL Server requires network target format (host:port or host), got: ${config.target}"
        }
    }
}

