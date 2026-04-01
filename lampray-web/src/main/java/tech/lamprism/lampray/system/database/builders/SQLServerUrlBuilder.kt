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
import tech.lamprism.lampray.system.database.DatabaseSslArtifacts
import tech.lamprism.lampray.system.database.DatabaseSslMode
import tech.lamprism.lampray.system.database.DatabaseSslSupport
import tech.lamprism.lampray.system.database.DatabaseType
import tech.lamprism.lampray.system.database.addResourceCleanupSuppressed

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

    override fun buildSslArtifacts(config: DatabaseConfig): DatabaseSslArtifacts {
        if (config.ssl.certificate != null || config.ssl.key != null) {
            throw IllegalArgumentException(
                "SQL Server managed SSL does not support client certificate material in this implementation."
            )
        }

        val properties = linkedMapOf<String, String>()
        val resources = mutableListOf<AutoCloseable>()

        when (config.ssl.mode) {
            DatabaseSslMode.DISABLED -> properties["encrypt"] = "false"
            DatabaseSslMode.REQUIRED -> {
                properties["encrypt"] = "true"
                properties["trustServerCertificate"] = "true"
            }
            DatabaseSslMode.VERIFY_IDENTITY -> {
                properties["encrypt"] = "true"
                properties["trustServerCertificate"] = "false"
            }
            DatabaseSslMode.VERIFY_CA -> {
                throw IllegalArgumentException(
                    "SQL Server does not support a managed verify-ca mode without hostname validation. " +
                            "Use 'required', 'verify-identity', or supplemental driver properties in database.options if you need custom trust behavior."
                )
            }
        }

        try {
            config.ssl.ca?.let { ca ->
                if (config.ssl.mode != DatabaseSslMode.VERIFY_IDENTITY) {
                    throw IllegalArgumentException(
                        "SQL Server custom CA material requires 'database.ssl.mode=verify-identity'."
                    )
                }
                val trustStore = DatabaseSslSupport.materializeTrustStore("sqlserver-trust", ca)
                properties["trustStore"] = trustStore.path.toAbsolutePath().toString()
                properties["trustStoreType"] = trustStore.type
                properties["trustStorePassword"] = trustStore.password
                resources.addAll(trustStore.resources)
            }
        } catch (e: Exception) {
            addResourceCleanupSuppressed(resources, e)
            throw e
        }

        return DatabaseSslArtifacts(properties, resources)
    }

    override fun getReservedSslOptionKeys(): Set<String> = setOf(
        "encrypt",
        "trustServerCertificate",
        "trustStore",
        "trustStoreType",
        "trustStorePassword"
    )

    override fun getDefaultValidationQuery(): String = "SELECT 1"

    override fun validateConfig(config: DatabaseConfig) {
        super.validateConfig(config)

        // SQL Server-specific validations
        require(config.target.isNetwork()) {
            "SQL Server requires network target format (host:port or host), got: ${config.target}"
        }
    }
}
