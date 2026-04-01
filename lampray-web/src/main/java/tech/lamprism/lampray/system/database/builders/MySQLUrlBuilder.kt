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
 * URL builder for MySQL and MariaDB databases.
 * Supports both MySQL and MariaDB with similar connection parameters.
 *
 * @author RollW
 */
class MySQLUrlBuilder : AbstractDatabaseUrlBuilder() {

    override val supportedTypes = setOf(DatabaseType.MYSQL, DatabaseType.MARIADB)

    override fun buildBaseUrl(config: DatabaseConfig): String {
        val target = config.target
        return if (target.isNetwork()) {
            val database = config.databaseName.ifBlank {
                throw IllegalArgumentException("Database name must be specified for MySQL or MariaDB")
            }
            "${config.type.urlPrefix}${target.getNetworkAddress()}/$database"
        } else {
            throw IllegalArgumentException("MySQL requires network target format (host:port or host)")
        }
    }

    override fun addCharsetParameter(params: MutableMap<String, String>, charset: String) {
        // MySQL uses characterEncoding parameter
        params["characterEncoding"] = charset
        // Also set useUnicode for proper Unicode support
        params["useUnicode"] = "true"
    }

    override fun buildSslArtifacts(config: DatabaseConfig): DatabaseSslArtifacts {
        return when (config.type) {
            DatabaseType.MYSQL -> buildMySqlSslArtifacts(config)
            DatabaseType.MARIADB -> buildMariaDbSslArtifacts(config)
            else -> DatabaseSslArtifacts.EMPTY
        }
    }

    override fun getReservedSslOptionKeys(): Set<String> = setOf(
        "sslMode",
        "useSSL",
        "requireSSL",
        "verifyServerCertificate",
        "trustServerCertificate",
        "disableSslHostnameVerification",
        "trustCertificateKeyStoreUrl",
        "trustCertificateKeyStoreType",
        "trustCertificateKeyStorePassword",
        "fallbackToSystemTrustStore",
        "clientCertificateKeyStoreUrl",
        "clientCertificateKeyStoreType",
        "clientCertificateKeyStorePassword",
        "fallbackToSystemKeyStore",
        "serverSslCert",
        "keyStore",
        "keyStorePassword",
        "keyPassword"
    )

    override fun getDefaultValidationQuery(): String = "SELECT 1"

    override fun validateConfig(config: DatabaseConfig) {
        super.validateConfig(config)

        // MySQL-specific validations
        require(config.target.isNetwork()) {
            "MySQL requires network target format (host:port or host), got: ${config.target}"
        }
    }

    private fun mapMySqlSslMode(config: DatabaseConfig): String {
        return when (config.ssl.mode) {
            tech.lamprism.lampray.system.database.DatabaseSslMode.DISABLED -> "DISABLED"
            tech.lamprism.lampray.system.database.DatabaseSslMode.REQUIRED -> "REQUIRED"
            tech.lamprism.lampray.system.database.DatabaseSslMode.VERIFY_CA -> "VERIFY_CA"
            tech.lamprism.lampray.system.database.DatabaseSslMode.VERIFY_IDENTITY -> "VERIFY_IDENTITY"
        }
    }

    private fun mapMariaDbSslMode(config: DatabaseConfig): String {
        return when (config.ssl.mode) {
            tech.lamprism.lampray.system.database.DatabaseSslMode.DISABLED -> "disable"
            tech.lamprism.lampray.system.database.DatabaseSslMode.REQUIRED -> "trust"
            tech.lamprism.lampray.system.database.DatabaseSslMode.VERIFY_CA -> "verify-ca"
            tech.lamprism.lampray.system.database.DatabaseSslMode.VERIFY_IDENTITY -> "verify-full"
        }
    }

    private fun buildMySqlSslArtifacts(config: DatabaseConfig): DatabaseSslArtifacts {
        val properties = linkedMapOf("sslMode" to mapMySqlSslMode(config))
        val resources = mutableListOf<AutoCloseable>()

        try {
            config.ssl.ca?.let { ca ->
                val trustStore = DatabaseSslSupport.materializeTrustStore("mysql-trust", ca)
                properties["trustCertificateKeyStoreUrl"] = trustStore.path.toUri().toString()
                properties["trustCertificateKeyStoreType"] = trustStore.type
                properties["trustCertificateKeyStorePassword"] = trustStore.password
                properties["fallbackToSystemTrustStore"] = "false"
                resources.addAll(trustStore.resources)
            }

            val clientCertificate = config.ssl.certificate
            val clientKey = config.ssl.key
            if (clientCertificate != null && clientKey != null) {
                val keyStore = DatabaseSslSupport.materializeKeyStore("mysql-client", clientCertificate, clientKey)
                properties["clientCertificateKeyStoreUrl"] = keyStore.path.toUri().toString()
                properties["clientCertificateKeyStoreType"] = keyStore.type
                properties["clientCertificateKeyStorePassword"] = keyStore.password
                properties["fallbackToSystemKeyStore"] = "false"
                resources.addAll(keyStore.resources)
            }
        } catch (e: Exception) {
            addResourceCleanupSuppressed(resources, e)
            throw e
        }

        return DatabaseSslArtifacts(properties, resources)
    }

    private fun buildMariaDbSslArtifacts(config: DatabaseConfig): DatabaseSslArtifacts {
        val properties = linkedMapOf("sslMode" to mapMariaDbSslMode(config))
        val resources = mutableListOf<AutoCloseable>()

        try {
            config.ssl.ca?.let { ca ->
                val certificate = DatabaseSslSupport.materializePemFile("mariadb-ca", ca)
                properties["serverSslCert"] = certificate.path.toAbsolutePath().toString()
                properties["fallbackToSystemTrustStore"] = "false"
                resources.addAll(certificate.resources)
            }

            val clientCertificate = config.ssl.certificate
            val clientKey = config.ssl.key
            if (clientCertificate != null && clientKey != null) {
                val keyStore = DatabaseSslSupport.materializeKeyStore(
                    prefix = "mariadb-client",
                    certificate = clientCertificate,
                    key = clientKey
                )
                properties["keyStore"] = keyStore.path.toAbsolutePath().toString()
                properties["keyStorePassword"] = keyStore.password
                properties["keyPassword"] = keyStore.password
                resources.addAll(keyStore.resources)
            }
        } catch (e: Exception) {
            addResourceCleanupSuppressed(resources, e)
            throw e
        }

        return DatabaseSslArtifacts(properties, resources)
    }
}
