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

package tech.lamprism.lampray.web.configuration.database

/**
 * Database connection configuration data class.
 *
 * @author RollW
 */
data class DatabaseConfig(
    val target: DatabaseTarget,
    val databaseName: String,
    val type: DatabaseType,
    val charset: String?,
    val customOptions: Collection<String>,
    val sslConfig: SslConfig,
    val connectionPoolConfig: ConnectionPoolConfig
)

/**
 * Supported database types with simplified classification.
 */
enum class DatabaseType(
    val typeName: String,
    val defaultPort: Int,
    val urlPrefix: String
) {
    MYSQL("mysql", 3306, "jdbc:mysql://"),
    MARIADB("mariadb", 3306, "jdbc:mariadb://"),
    POSTGRESQL("postgresql", 5432, "jdbc:postgresql://"),
    SQL_SERVER("sqlserver", 1433, "jdbc:sqlserver://"),
    ORACLE("oracle", 1521, "jdbc:oracle:thin:@//"),
    SQLITE("sqlite", 0, "jdbc:sqlite:"),
    H2("h2", 9092, "jdbc:h2:");

    companion object {
        fun fromString(type: String): DatabaseType {
            return entries.find { it.typeName.equals(type, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported database type: $type")
        }
    }
}

/**
 * SSL/TLS configuration for database connections.
 * SSL certificates are configured separately from the JDBC URL and applied to the connection pool.
 *
 * @author RollW
 */
data class SslConfig(
    val mode: Mode = Mode.DISABLE,
    val clientCertificate: CertificateValue? = null,
    val clientPrivateKey: CertificateValue? = null,
    val caCertificate: CertificateValue? = null,
    val verifyServerCertificate: Boolean = true,
    val allowAllCertificates: Boolean = false
) {
    enum class Mode {
        /**
         * SSL connection is disabled
         */
        DISABLE,

        /**
         * SSL connection is preferred but not required
         */
        PREFER,

        /**
         * SSL connection is required
         */
        REQUIRE,

        /**
         * SSL connection is required with certificate verification
         */
        VERIFY_CA,

        /**
         * SSL connection is required with full certificate and hostname verification
         */
        VERIFY_IDENTITY
    }

    fun isEnabled(): Boolean {
        return mode != Mode.DISABLE
    }
}

/**
 * Connection pool configuration for database connections.
 *
 * @param initialSize Initial number of connections in the pool
 * @param maxActive Maximum number of active connections in the pool
 * @param maxIdle Maximum number of idle connections in the pool
 * @param minIdle Minimum number of idle connections in the pool
 * @param maxWait Maximum wait time (in milliseconds) when getting a connection from the pool
 * @param connectionTimeout Connection timeout (in milliseconds)
 * @param testOnBorrow Whether to validate connections before borrowing from the pool
 * @param testOnReturn Whether to validate connections before returning to the pool
 * @param testWhileIdle Whether to validate idle connections
 * @param timeBetweenEvictionRuns Time between eviction runs (in milliseconds)
 * @param minEvictableIdleTime Minimum time a connection may sit idle before being evicted (in milliseconds)
 * @param removeAbandoned Whether to remove abandoned connections
 * @param removeAbandonedTimeout Timeout for abandoned connections (in seconds)
 * @param logAbandoned Whether to log abandoned connections
 * @author RollW
 */
data class ConnectionPoolConfig(
    val initialSize: Int = 5,
    val maxActive: Int = 20,
    val maxIdle: Int = 10,
    val minIdle: Int = 2,
    val maxWait: Long = 30000,
    val connectionTimeout: Long = 30000,
    val testOnBorrow: Boolean = true,
    val testOnReturn: Boolean = false,
    val testWhileIdle: Boolean = true,
    val timeBetweenEvictionRuns: Long = 60000,
    val minEvictableIdleTime: Long = 300000,
    val removeAbandoned: Boolean = true,
    val removeAbandonedTimeout: Int = 300,
    val logAbandoned: Boolean = false
)
