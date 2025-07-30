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

import tech.lamprism.lampray.web.configuration.database.CertificateValue
import tech.lamprism.lampray.web.configuration.database.DatabaseConfig
import tech.lamprism.lampray.web.configuration.database.DatabaseType
import tech.lamprism.lampray.web.configuration.database.SslConfig

/**
 * URL builder for PostgreSQL databases.
 * Handles PostgreSQL-specific connection parameters and SSL configuration.
 *
 * @author RollW
 */
class PostgreSQLUrlBuilder : AbstractDatabaseUrlBuilder() {

    override val supportedTypes = setOf(DatabaseType.POSTGRESQL)

    override fun buildBaseUrl(config: DatabaseConfig): String {
        val target = config.target

        return if (target.isNetwork()) {
            val database = config.databaseName.ifBlank {
                throw IllegalArgumentException("Database name must be specified for PostgreSQL")
            }
            "${config.type.urlPrefix}${target.getNetworkAddress()}/$database"
        } else {
            throw IllegalArgumentException("PostgreSQL requires network target format (host:port or host)")
        }
    }

    override fun addCharsetParameter(params: MutableMap<String, String>, charset: String) {
        // PostgreSQL uses different charset parameter names
        when (charset.lowercase()) {
            "utf8", "utf-8" -> params["characterEncoding"] = "UTF8"
            "utf8mb4" -> params["characterEncoding"] = "UTF8"
            else -> params["characterEncoding"] = charset.uppercase()
        }
    }

    override fun addSslParameters(params: MutableMap<String, String>, config: DatabaseConfig) {
        when (config.sslConfig.mode) {
            SslConfig.Mode.DISABLE -> {
                params["ssl"] = "false"
            }
            SslConfig.Mode.PREFER -> {
                // PostgreSQL default behavior - try SSL first, fallback to non-SSL
                // No explicit parameter needed
            }
            SslConfig.Mode.REQUIRE -> {
                params["ssl"] = "true"
                params["sslmode"] = "require"
            }
            SslConfig.Mode.VERIFY_CA -> {
                params["ssl"] = "true"
                params["sslmode"] = "verify-ca"
            }
            SslConfig.Mode.VERIFY_IDENTITY -> {
                params["ssl"] = "true"
                params["sslmode"] = "verify-full"
            }
        }

        // Handle certificate configuration
        config.sslConfig.clientCertificate?.let {
            if (!it.isEmpty()) {
                when (it.type) {
                    CertificateValue.CertificateType.PATH -> {
                        params["sslcert"] = it.value
                    }
                    else -> {
                        // For content-based certificates, we'll need to write to temp files
                        // This is handled at the connection pool level
                    }
                }
            }
        }

        config.sslConfig.clientPrivateKey?.let {
            if (!it.isEmpty()) {
                when (it.type) {
                    CertificateValue.CertificateType.PATH -> {
                        params["sslkey"] = it.value
                    }
                    else -> {
                        // For content-based keys, handled at connection pool level
                    }
                }
            }
        }

        config.sslConfig.caCertificate?.let {
            if (!it.isEmpty()) {
                when (it.type) {
                    CertificateValue.CertificateType.PATH -> {
                        params["sslrootcert"] = it.value
                    }
                    else -> {
                        // For content-based CA certs, handled at connection pool level
                    }
                }
            }
        }
    }

    override fun getDefaultValidationQuery(): String = "SELECT 1"

    override fun validateConfig(config: DatabaseConfig) {
        super.validateConfig(config)

        // PostgreSQL-specific validations
        val target = config.target
        require(target.isNetwork()) {
            "PostgreSQL requires network target format (host:port or host), got: ${config.target}"
        }
    }
}
