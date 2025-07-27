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
import tech.lamprism.lampray.setting.SettingDescription
import tech.lamprism.lampray.setting.SettingKey
import tech.lamprism.lampray.setting.SettingSource
import tech.lamprism.lampray.setting.SettingSpecificationBuilder
import tech.lamprism.lampray.setting.SettingSpecificationSupplier

/**
 * Database configuration keys with streamlined options and clear descriptions.
 *
 * Configuration is simplified with a unified connection target that supports
 * file paths, database addresses, and memory configurations.
 *
 * @author RollW
 */
object DatabaseConfigKeys : SettingSpecificationSupplier {

    private val LOCAL_SOURCE = SettingSource.LOCAL_ONLY

    // Basic Connection Settings

    /**
     * Database type selection for JDBC connection establishment.
     * Determines the JDBC driver and URL prefix to use for database connectivity.
     * Supported values: mysql, mariadb, postgresql, sqlserver, oracle, sqlite, h2
     */
    @JvmField
    val DATABASE_TYPE = SettingSpecificationBuilder(SettingKey.ofString("database.type"))
        .setDescription(SettingDescription.text("Select database type, e.g., mysql, postgresql, sqlite. Default is sqlite."))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("sqlite") // Default to SQLite for simplicity
        .setRequired(true)
        .build()

    /**
     * Unified database connection target supporting multiple connection methods:
     * - Network connections: "localhost:3306", "db.example.com:5432", "localhost" (uses default port)
     * - File-based databases: "file:./data/app.db", "file:/absolute/path/to/database.db"
     * - In-memory databases: "memory"
     * The system automatically detects the target type and applies appropriate connection logic.
     */
    @JvmField
    val DATABASE_TARGET = SettingSpecificationBuilder(SettingKey.ofString("database.target"))
        .setDescription(SettingDescription.text("Database connection target (host:port, file:path, or memory)"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("memory")
        .setRequired(true)
        .setAllowAnyValue(true)
        .build()

    /**
     * Database authentication username.
     * Used for database login credentials when connecting to network-based databases.
     */
    @JvmField
    val DATABASE_USERNAME = SettingSpecificationBuilder(SettingKey.ofString("database.username"))
        .setDescription(SettingDescription.text("Database login username"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("root")
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Database authentication password.
     * Sensitive credential used for database authentication. Consider using environment variables.
     */
    @JvmField
    val DATABASE_PASSWORD = SettingSpecificationBuilder(SettingKey.ofString("database.password"))
        .setDescription(SettingDescription.text("Database login password"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("")
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Target database name within the database server.
     * For network databases, this specifies which database to connect to on the server.
     * Not applicable for file-based or in-memory databases.
     */
    @JvmField
    val DATABASE_NAME = SettingSpecificationBuilder(SettingKey.ofString("database.name"))
        .setDescription(SettingDescription.text("Database name to connect to"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("")
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    // Character Set and Encoding

    /**
     * Database character encoding specification for proper text handling.
     * Common values include utf8, utf8mb4 (MySQL), UTF8 (PostgreSQL).
     * If not specified, the database server's default encoding will be used.
     */
    @JvmField
    val DATABASE_CHARSET = SettingSpecificationBuilder(SettingKey.ofString("database.charset"))
        .setDescription(SettingDescription.text("Character encoding (utf8, utf8mb4, etc.)"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue(null)
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    // Custom Options

    /**
     * Additional database-specific JDBC connection parameters.
     * These parameters are appended to the JDBC URL as query parameters.
     * Format: "key1=value1&key2=value2" (URL parameter format without leading "?")
     * Example: "timezone=UTC&allowMultiQueries=true"
     */
    @JvmField
    val DATABASE_OPTIONS = SettingSpecificationBuilder(SettingKey.ofString("database.options"))
        .setDescription(SettingDescription.text("Additional JDBC parameters (key1=value1&key2=value2)"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("")
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    // SSL/TLS Configuration

    /**
     * Enable SSL/TLS encryption for database connections.
     * When enabled, the connection will use encrypted communication with the database server.
     */
    @JvmField
    val DATABASE_SSL_ENABLED = SettingSpecificationBuilder(SettingKey.ofBoolean("database.ssl.enabled"))
        .setDescription(SettingDescription.text("Enable SSL/TLS encryption"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue(false)
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * SSL connection security level and certificate verification mode.
     * - disable: No SSL encryption
     * - prefer: Try SSL first, fallback to non-SSL if failed
     * - require: SSL required, connection fails if SSL unavailable
     * - verify-ca: SSL required with CA certificate verification
     * - verify-identity: SSL required with full certificate and hostname verification
     */
    @JvmField
    val DATABASE_SSL_MODE = SettingSpecificationBuilder(SettingKey.ofString("database.ssl.mode"))
        .setDescription(SettingDescription.text("SSL security level"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("prefer")
        .setValueEntries(listOf("disable", "prefer", "require", "verify-ca", "verify-identity"))
        .setRequired(false)
        .build()

    /**
     * Client certificate content in PEM format for mutual TLS authentication.
     * Contains the full certificate text including "-----BEGIN CERTIFICATE-----" markers.
     * Alternative to using certificate file path - use either content or path, not both.
     */
    @JvmField
    val DATABASE_SSL_CLIENT_CERT = SettingSpecificationBuilder(SettingKey.ofString("database.ssl.client.cert"))
        .setDescription(SettingDescription.text("Client certificate content (PEM format)"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("")
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * File system path to client certificate file for mutual TLS authentication.
     * Should point to a valid PEM-formatted certificate file.
     * Alternative to certificate content - use either content or path, not both.
     */
    @JvmField
    val DATABASE_SSL_CLIENT_CERT_PATH = SettingSpecificationBuilder(SettingKey.ofString("database.ssl.client.cert.path"))
        .setDescription(SettingDescription.text("Client certificate file path"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("")
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Client private key content in PEM format for mutual TLS authentication.
     * Contains the private key text including "-----BEGIN PRIVATE KEY-----" markers.
     * Must correspond to the client certificate. Use either content or path, not both.
     */
    @JvmField
    val DATABASE_SSL_CLIENT_KEY = SettingSpecificationBuilder(SettingKey.ofString("database.ssl.client.key"))
        .setDescription(SettingDescription.text("Client private key content (PEM format)"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("")
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * File system path to client private key file for mutual TLS authentication.
     * Should point to a valid PEM-formatted private key file.
     * Must correspond to the client certificate. Use either content or path, not both.
     */
    @JvmField
    val DATABASE_SSL_CLIENT_KEY_PATH = SettingSpecificationBuilder(SettingKey.ofString("database.ssl.client.key.path"))
        .setDescription(SettingDescription.text("Client private key file path"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("")
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Certificate Authority (CA) certificate content in PEM format for server verification.
     * Used to verify the database server's SSL certificate against a trusted CA.
     * Alternative to using CA certificate file path - use either content or path, not both.
     */
    @JvmField
    val DATABASE_SSL_CA_CERT = SettingSpecificationBuilder(SettingKey.ofString("database.ssl.ca.cert"))
        .setDescription(SettingDescription.text("CA certificate content for server verification (PEM format)"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("")
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * File system path to Certificate Authority (CA) certificate for server verification.
     * Should point to a valid PEM-formatted CA certificate file.
     * Alternative to CA certificate content - use either content or path, not both.
     */
    @JvmField
    val DATABASE_SSL_CA_CERT_PATH = SettingSpecificationBuilder(SettingKey.ofString("database.ssl.ca.cert.path"))
        .setDescription(SettingDescription.text("CA certificate file path for server verification"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("")
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Enable server certificate verification for SSL connections.
     * When true, the client verifies the server's SSL certificate.
     * Set to false only for testing with self-signed or invalid certificates.
     */
    @JvmField
    val DATABASE_SSL_VERIFY_SERVER = SettingSpecificationBuilder(SettingKey.ofBoolean("database.ssl.verify.server"))
        .setDescription(SettingDescription.text("Verify server SSL certificate"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue(true)
        .setRequired(false)
        .build()

    /**
     * Allow connections to servers with self-signed SSL certificates.
     * When enabled, bypasses certificate chain validation for development/testing.
     * Should only be enabled in non-production environments for security reasons.
     */
    @JvmField
    val DATABASE_SSL_ALLOW_SELF_SIGNED = SettingSpecificationBuilder(SettingKey.ofBoolean("database.ssl.allow.self.signed"))
        .setDescription(SettingDescription.text("Allow self-signed certificates (development only)"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue(false)
        .setRequired(false)
        .build()

    // Connection Pool Configuration

    /**
     * Maximum number of concurrent database connections in the connection pool.
     * Higher values allow more concurrent database operations but consume more resources.
     * Recommended range: 10-50 for most applications.
     */
    @JvmField
    val DATABASE_POOL_MAX_SIZE = SettingSpecificationBuilder(SettingKey.ofInt("database.pool.max.size"))
        .setDescription(SettingDescription.text("Maximum connections in pool"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue(20)
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Minimum number of idle connections maintained in the pool.
     * Ensures quick response times by keeping connections ready for use.
     * Should be less than or equal to maximum pool size.
     */
    @JvmField
    val DATABASE_POOL_MIN_IDLE = SettingSpecificationBuilder(SettingKey.ofInt("database.pool.min.idle"))
        .setDescription(SettingDescription.text("Minimum idle connections in pool"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue(2)
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Maximum time to wait for a database connection from the pool (in milliseconds).
     * If no connection is available within this time, an exception is thrown.
     * Common values: 10000-30000ms (10-30 seconds).
     */
    @JvmField
    val DATABASE_POOL_CONNECTION_TIMEOUT = SettingSpecificationBuilder(SettingKey.ofLong("database.pool.connection.timeout"))
        .setDescription(SettingDescription.text("Connection timeout (milliseconds)"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue(30000L)
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Maximum lifetime of a database connection before it's retired (in milliseconds).
     * Prevents issues with long-lived connections and helps with connection pool health.
     * Recommended: 1800000ms (30 minutes) to 3600000ms (1 hour).
     */
    @JvmField
    val DATABASE_POOL_MAX_LIFETIME = SettingSpecificationBuilder(SettingKey.ofLong("database.pool.max.lifetime"))
        .setDescription(SettingDescription.text("Maximum connection lifetime (milliseconds)"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue(1800000L) // 30 minutes
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Time a connection can remain idle before being eligible for eviction (in milliseconds).
     * Helps maintain an optimal number of active connections in the pool.
     * Common values: 300000-600000ms (5-10 minutes).
     */
    @JvmField
    val DATABASE_POOL_IDLE_TIMEOUT = SettingSpecificationBuilder(SettingKey.ofLong("database.pool.idle.timeout"))
        .setDescription(SettingDescription.text("Idle connection timeout (milliseconds)"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue(600000L) // 10 minutes
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Connection leak detection threshold in milliseconds (0 to disable).
     * When enabled, logs warnings if connections are held longer than this threshold.
     * Useful for debugging connection leaks. Set to 0 to disable leak detection.
     */
    @JvmField
    val DATABASE_POOL_LEAK_DETECTION_THRESHOLD = SettingSpecificationBuilder(SettingKey.ofLong("database.pool.leak.detection.threshold"))
        .setDescription(SettingDescription.text("Leak detection threshold (milliseconds, 0=disabled)"))
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue(0L)
        .setRequired(false)
        .build()


    override val specifications: List<AttributedSettingSpecification<*, *>> = listOf(
        DATABASE_TYPE,
        DATABASE_TARGET,
        DATABASE_USERNAME,
        DATABASE_PASSWORD,
        DATABASE_NAME,
        DATABASE_CHARSET,
        DATABASE_OPTIONS,
        DATABASE_SSL_ENABLED,
        DATABASE_SSL_MODE,
        DATABASE_SSL_CLIENT_CERT,
        DATABASE_SSL_CLIENT_CERT_PATH,
        DATABASE_SSL_CLIENT_KEY,
        DATABASE_SSL_CLIENT_KEY_PATH,
        DATABASE_SSL_CA_CERT,
        DATABASE_SSL_CA_CERT_PATH,
        DATABASE_SSL_VERIFY_SERVER,
        DATABASE_SSL_ALLOW_SELF_SIGNED,
        DATABASE_POOL_MAX_SIZE,
        DATABASE_POOL_MIN_IDLE,
        DATABASE_POOL_CONNECTION_TIMEOUT,
        DATABASE_POOL_MAX_LIFETIME,
        DATABASE_POOL_IDLE_TIMEOUT,
        DATABASE_POOL_LEAK_DETECTION_THRESHOLD
    )
}
