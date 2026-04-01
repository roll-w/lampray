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
import tech.lamprism.lampray.system.database.DatabaseSslSupport
import tech.lamprism.lampray.system.database.DatabaseType
import tech.lamprism.lampray.system.database.addResourceCleanupSuppressed

/**
 * URL builder for PostgreSQL database.
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

    override fun buildSslArtifacts(config: DatabaseConfig): DatabaseSslArtifacts {
        val properties = linkedMapOf(
            "sslmode" to when (config.ssl.mode) {
                tech.lamprism.lampray.system.database.DatabaseSslMode.DISABLED -> "disable"
                tech.lamprism.lampray.system.database.DatabaseSslMode.REQUIRED -> "require"
                tech.lamprism.lampray.system.database.DatabaseSslMode.VERIFY_CA -> "verify-ca"
                tech.lamprism.lampray.system.database.DatabaseSslMode.VERIFY_IDENTITY -> "verify-full"
            }
        )
        val resources = mutableListOf<AutoCloseable>()

        try {
            config.ssl.ca?.let { ca ->
                val artifact = DatabaseSslSupport.materializePemFile("postgres-ca", ca)
                properties["sslrootcert"] = artifact.path.toAbsolutePath().toString()
                resources.addAll(artifact.resources)
            }

            val clientCertificate = config.ssl.certificate
            val clientKey = config.ssl.key
            if (clientCertificate != null && clientKey != null) {
                val certArtifact = DatabaseSslSupport.materializePemFile("postgres-cert", clientCertificate)
                resources.addAll(certArtifact.resources)
                properties["sslcert"] = certArtifact.path.toAbsolutePath().toString()

                val keyArtifact = DatabaseSslSupport.materializePemFile("postgres-key", clientKey)
                properties["sslkey"] = keyArtifact.path.toAbsolutePath().toString()
                resources.addAll(keyArtifact.resources)
            }
        } catch (e: Exception) {
            addResourceCleanupSuppressed(resources, e)
            throw e
        }

        return DatabaseSslArtifacts(properties, resources)
    }

    override fun getReservedSslOptionKeys(): Set<String> = setOf(
        "ssl",
        "sslmode",
        "sslrootcert",
        "sslcert",
        "sslkey",
        "sslfactory",
        "sslfactoryarg",
        "sslhostnameverifier",
        "sslnegotiation"
    )

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
