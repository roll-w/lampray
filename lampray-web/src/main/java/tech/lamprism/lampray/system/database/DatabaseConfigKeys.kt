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

package tech.lamprism.lampray.system.database

import org.springframework.stereotype.Component
import tech.lamprism.lampray.setting.AttributedSettingSpecification
import tech.lamprism.lampray.setting.SettingDescription
import tech.lamprism.lampray.setting.SettingKey
import tech.lamprism.lampray.setting.SettingSource
import tech.lamprism.lampray.setting.SettingSpecificationBuilder
import tech.lamprism.lampray.setting.SettingSpecificationSupplier

/**
 * Configuration keys for database connection and pool settings.
 *
 * @author RollW
 */
@Component
object DatabaseConfigKeys : SettingSpecificationSupplier {

    private val LOCAL_SOURCE = SettingSource.LOCAL_ONLY

    // Basic Connection Settings

    /**
     * Database type selection for establishing JDBC connections.
     *
     * Specifies which database system to connect to, determining the appropriate
     * JDBC driver and URL format. The system will automatically load the corresponding
     * driver based on this selection.
     */
    @JvmField
    val DATABASE_TYPE = SettingSpecificationBuilder(SettingKey.ofString("database.type"))
        .setDescription(
            SettingDescription.text(
                "Database type selection. Determines which database system to connect to and " +
                        "automatically loads the appropriate driver and URL format based on this selection."
            )
        )
        .setSupportedSources(LOCAL_SOURCE)
        .setValueEntries(listOf("sqlite", "mysql", "postgresql", "h2", "oracle", "sqlserver", "mariadb"))
        .setDefaultValue("sqlite") // Default to SQLite for simplicity
        .setRequired(true)
        .build()

    /**
     * Unified database connection target with flexible addressing support.
     *
     * Supports multiple connection methods through intelligent target detection:
     * - Network databases: "localhost:3306", "db.example.com:5432"
     * - File-based databases: "file:./data/app.db", "file:/absolute/path/to/db"
     * - In-memory databases: "memory"
     *
     * Technical implementation: Parsed by DatabaseTarget class to determine
     * connection type and generate appropriate JDBC URLs.
     */
    @JvmField
    val DATABASE_TARGET = SettingSpecificationBuilder(SettingKey.ofString("database.target"))
        .setDescription(
            SettingDescription.text(
                "Database connection address with flexible format support. " +
                        "Accepts network addresses (host:port), file paths (file:path), or memory mode. " +
                        "The system automatically detects the connection type and generates appropriate JDBC URLs.\n\n" +
                        "- Network databases: \"localhost:3306\", \"db.example.com:5432\"\n" +
                        "- File-based databases: \"file:./data/app.db\", \"file:/absolute/path/to/db\"\n" +
                        "- In-memory databases: \"memory\""
            )
        )
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("memory")
        .setRequired(true)
        .setAllowAnyValue(true)
        .build()

    /**
     * Database authentication username for login credentials.
     *
     * Used for authenticating with network-based database servers.
     * Not required for file-based or in-memory databases.
     */
    @JvmField
    val DATABASE_USERNAME = SettingSpecificationBuilder(SettingKey.ofString("database.username"))
        .setDescription(
            SettingDescription.text(
                "Database login username. Required for network-based database servers " +
                        "but not needed for file-based or in-memory databases."
            )
        )
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("root")
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Database authentication password for secure access.
     *
     * Provides authentication credentials for database login. Consider using
     * environment variables or secure configuration management for sensitive passwords.
     */
    @JvmField
    val DATABASE_PASSWORD = SettingSpecificationBuilder(SettingKey.ofString("database.password"))
        .setDescription(
            SettingDescription.text(
                "Database login password. Provides authentication credentials for database access. " +
                        "Consider using environment variables for sensitive passwords in production."
            )
        )
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("")
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Target database name within the database server instance.
     *
     * Specifies which specific database to use on multi-database servers.
     * Only applicable for network-based databases; ignored for file-based
     * and in-memory databases.
     */
    @JvmField
    val DATABASE_NAME = SettingSpecificationBuilder(SettingKey.ofString("database.name"))
        .setDescription(
            SettingDescription.text(
                "Target database name on the server. Specifies which specific database to use " +
                        "on multi-database servers. Only applicable for network-based databases. " +
                        "Defaults to 'lampray' database, needs to be created manually if not exists, " +
                        "will not be created automatically."
            )
        )
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("lampray")
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    // Character Set and Encoding

    /**
     * Database character encoding for proper internationalization support.
     *
     * Ensures correct handling of international characters and emojis.
     * Common values: utf8 (general).
     * Leave empty to use database server defaults.
     */
    @JvmField
    val DATABASE_CHARSET = SettingSpecificationBuilder(SettingKey.ofString("database.charset"))
        .setDescription(
            SettingDescription.text(
                "Database character encoding setting. Ensures correct handling of international " +
                        "characters and emojis. " +
                        "Leave empty to use database defaults."
            )
        )
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue(null)
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    // Custom Options

    /**
     * Additional JDBC connection parameters for fine-tuning database behavior.
     *
     * Allows specifying database-specific connection options not covered by
     * standard settings. Format as URL query parameters without leading "?".
     * Example: ["timezone=UTC", "allowMultiQueries=true", "useSSL=false"]
     */
    @JvmField
    val DATABASE_OPTIONS = SettingSpecificationBuilder(SettingKey.ofStringSet("database.options"))
        .setDescription(
            SettingDescription.text(
                "Additional JDBC connection parameters. Allows specifying database-specific " +
                        "options not covered by standard settings. " +
                        "(e.g., [\"timezone=UTC\", \"allowMultiQueries=true\", \"useSSL=false\"])."
            )
        )
        .setSupportedSources(LOCAL_SOURCE)
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    // Connection Pool Configuration

    /**
     * Maximum number of concurrent database connections in the pool.
     *
     * Controls the upper limit of simultaneous database connections.
     * Higher values support more concurrent operations but consume more resources.
     * Typical range: 10-50 for most applications.
     */
    @JvmField
    val DATABASE_POOL_MAX_SIZE = SettingSpecificationBuilder(SettingKey.ofInt("database.pool.max.size"))
        .setDescription(
            SettingDescription.text(
                "Maximum number of database connections in the pool. Controls the upper limit " +
                        "of simultaneous database connections. Higher values support more concurrency but consume more resources."
            )
        )
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue(20)
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Minimum number of idle connections maintained in the pool.
     *
     * Ensures responsive performance by keeping connections ready for immediate use.
     * Should be set based on typical concurrent load. Must not exceed max pool size.
     */
    @JvmField
    val DATABASE_POOL_MIN_IDLE = SettingSpecificationBuilder(SettingKey.ofInt("database.pool.min.idle"))
        .setDescription(
            SettingDescription.text(
                "Minimum number of idle connections in the pool. Ensures responsive performance " +
                        "by keeping connections ready for immediate use. Should be based on typical concurrent load."
            )
        )
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue(2)
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Maximum wait time for acquiring a connection from the pool.
     *
     * Time limit in milliseconds before throwing an exception when no connections
     * are available. Prevents indefinite blocking during high load periods.
     * Typical values: 10-30 seconds (10000-30000ms).
     */
    @JvmField
    val DATABASE_POOL_CONNECTION_TIMEOUT =
        SettingSpecificationBuilder(SettingKey.ofLong("database.pool.connection.timeout"))
            .setDescription(
                SettingDescription.text(
                    "Maximum wait time for acquiring a connection in milliseconds. " +
                            "Time limit before throwing an exception when no connections are available. " +
                            "Prevents indefinite blocking during high load."
                )
            )
            .setSupportedSources(LOCAL_SOURCE)
            .setDefaultValue(30000L)
            .setRequired(false)
            .setAllowAnyValue(true)
            .build()

    /**
     * Maximum lifetime of a database connection before forced retirement.
     *
     * Prevents issues with long-lived connections and ensures pool health.
     * Connections are retired gracefully before reaching this limit.
     * Recommended: 30 minutes to 1 hour (1800000-3600000ms).
     */
    @JvmField
    val DATABASE_POOL_MAX_LIFETIME = SettingSpecificationBuilder(SettingKey.ofLong("database.pool.max.lifetime"))
        .setDescription(
            SettingDescription.text(
                "Maximum lifetime of a connection before retirement in milliseconds. " +
                        "Prevents issues with long-lived connections and ensures pool health. " +
                        "Connections are retired gracefully before reaching this limit."
            )
        )
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue(1800000L) // 30 minutes
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Idle timeout before connections become eligible for eviction.
     *
     * Time in milliseconds a connection can remain unused before being closed.
     * Helps maintain optimal pool size by removing excess idle connections.
     * Typical values: 5-10 minutes (300000-600000ms).
     */
    @JvmField
    val DATABASE_POOL_IDLE_TIMEOUT = SettingSpecificationBuilder(SettingKey.ofLong("database.pool.idle.timeout"))
        .setDescription(
            SettingDescription.text(
                "Idle timeout before connection eviction in milliseconds. " +
                        "Time a connection can remain unused before being closed. " +
                        "Helps maintain optimal pool size by removing excess idle connections."
            )
        )
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue(600000L) // 10 minutes
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Connection leak detection threshold for debugging purposes.
     *
     * Logs warnings when connections are held longer than this threshold,
     * helping identify potential connection leaks in application code.
     * Set to 0 to disable leak detection entirely.
     */
    @JvmField
    val DATABASE_POOL_LEAK_DETECTION_THRESHOLD =
        SettingSpecificationBuilder(SettingKey.ofLong("database.pool.leak.detection.threshold"))
            .setDescription(
                SettingDescription.text(
                    "Connection leak detection threshold in milliseconds. " +
                            "Logs warnings when connections are held longer than this threshold to help identify potential leaks. " +
                            "Set to 0 to disable detection."
                )
            )
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
        DATABASE_POOL_MAX_SIZE,
        DATABASE_POOL_MIN_IDLE,
        DATABASE_POOL_CONNECTION_TIMEOUT,
        DATABASE_POOL_MAX_LIFETIME,
        DATABASE_POOL_IDLE_TIMEOUT,
        DATABASE_POOL_LEAK_DETECTION_THRESHOLD
    )
}