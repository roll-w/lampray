package tech.lamprism.lampray.web.configuration.database

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
     * Database type selection for establishing JDBC connections.
     *
     * Specifies which database system to connect to, determining the appropriate
     * JDBC driver and URL format. The system will automatically load the corresponding
     * driver based on this selection.
     */
    @JvmField
    val DATABASE_TYPE = SettingSpecificationBuilder(SettingKey.Companion.ofString("database.type"))
        .setDescription(
            SettingDescription.Companion.text(
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
    val DATABASE_TARGET = SettingSpecificationBuilder(SettingKey.Companion.ofString("database.target"))
        .setDescription(
            SettingDescription.Companion.text(
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
    val DATABASE_USERNAME = SettingSpecificationBuilder(SettingKey.Companion.ofString("database.username"))
        .setDescription(
            SettingDescription.Companion.text(
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
    val DATABASE_PASSWORD = SettingSpecificationBuilder(SettingKey.Companion.ofString("database.password"))
        .setDescription(
            SettingDescription.Companion.text(
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
    val DATABASE_NAME = SettingSpecificationBuilder(SettingKey.Companion.ofString("database.name"))
        .setDescription(
            SettingDescription.Companion.text(
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
     * Common values: utf8mb4 (MySQL), UTF8 (PostgreSQL), utf8 (general).
     * Leave empty to use database server defaults.
     */
    @JvmField
    val DATABASE_CHARSET = SettingSpecificationBuilder(SettingKey.Companion.ofString("database.charset"))
        .setDescription(
            SettingDescription.Companion.text(
                "Database character encoding setting. Ensures correct handling of international " +
                        "characters and emojis. Common values include utf8mb4 (MySQL), UTF8 (PostgreSQL), or utf8 (general). " +
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
    val DATABASE_OPTIONS = SettingSpecificationBuilder(SettingKey.Companion.ofStringSet("database.options"))
        .setDescription(
            SettingDescription.Companion.text(
                "Additional JDBC connection parameters. Allows specifying database-specific " +
                        "options not covered by standard settings. " +
                        "(e.g., timezone=UTC,allowMultiQueries=true)."
            )
        )
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("")
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    // SSL/TLS Configuration

    /**
     * SSL connection security level and certificate verification strictness.
     *
     * Controls how strictly SSL certificates are validated:
     * - disable: No SSL encryption
     * - prefer: Try SSL first, fallback to non-SSL
     * - require: SSL required, connection fails without it
     * - verify-ca: SSL required with CA certificate verification
     * - verify-identity: SSL required with full certificate and hostname verification
     */
    @JvmField
    val DATABASE_SSL_MODE = SettingSpecificationBuilder(SettingKey.Companion.ofString("database.ssl.mode"))
        .setDescription(
            SettingDescription.Companion.text(
                "SSL security level and certificate verification strictness. " +
                        "Recommended enabled SSL/TLS for production and remote connections. " +
                        "Use mode controls how strictly SSL certificates are validated, ranging from 'disable' (no SSL) " +
                        "to 'verify-identity' (full certificate and hostname verification):\n\n" +
                        "- disable: No SSL encryption\n" +
                        "- prefer: Try SSL first, fallback to non-SSL\n" +
                        "- require: SSL required, connection fails without it\n" +
                        "- verify-ca: SSL required with CA certificate verification\n" +
                        "- verify-identity: SSL required with full certificate and hostname verification"
            )
        )
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("disable")
        .setValueEntries(listOf("disable", "prefer", "require", "verify-ca", "verify-identity"))
        .setRequired(false)
        .build()

    /**
     * Client certificate for mutual TLS authentication with flexible input support.
     *
     * Supports both PEM certificate content and file path through automatic detection:
     * - PEM content: Provide certificate directly with BEGIN/END markers
     * - File path: Provide path to PEM-formatted certificate file
     *
     * The system automatically detects the input format based on PEM headers.
     * For PEM content, include the full certificate with -----BEGIN CERTIFICATE----- markers.
     * For file paths, ensure the file is accessible and contains valid PEM format.
     */
    @JvmField
    val DATABASE_SSL_CLIENT_CERT = SettingSpecificationBuilder(SettingKey.Companion.ofString("database.ssl.client.cert"))
        .setDescription(
            SettingDescription.Companion.text(
                "Client certificate for mutual TLS authentication with flexible input support. " +
                        "Supports both PEM certificate content and file path through automatic detection:\n\n" +
                        "- PEM content: Provide PEM formated certificate directly with BEGIN/END markers\n" +
                        "- File path: Provide path to PEM-formatted certificate file\n\n" +
                        "The system automatically detects the input format based on PEM headers."
            )
        )
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("")
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Client private key for mutual TLS authentication with flexible input support.
     *
     * Supports both PEM private key content and file path through automatic detection:
     * - PEM content: Provide private key directly with BEGIN/END markers
     * - File path: Provide path to PEM-formatted private key file
     *
     * The system automatically detects the input format based on PEM headers.
     * Must correspond to the client certificate for proper authentication.
     */
    @JvmField
    val DATABASE_SSL_CLIENT_KEY = SettingSpecificationBuilder(SettingKey.Companion.ofString("database.ssl.client.key"))
        .setDescription(
            SettingDescription.Companion.text(
                "Client private key for mutual TLS authentication with flexible input support. " +
                        "Supports both PEM private key content and file path through automatic detection:\n\n" +
                        "- PEM content: Provide PEM formated private key directly with BEGIN/END markers\n" +
                        "- File path: Provide path to PEM-formatted private key file\n\n" +
                        "The system automatically detects the input format based on PEM headers. " +
                        "Must correspond to the client certificate for proper authentication."
            )
        )
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("")
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Certificate Authority (CA) certificate for server verification with flexible input support.
     *
     * Supports both PEM certificate content and file path through automatic detection:
     * - PEM content: Provide CA certificate directly with BEGIN/END markers
     * - File path: Provide path to PEM-formatted CA certificate file
     *
     * The system automatically detects the input format based on PEM headers.
     * Required when using custom or self-signed certificates for server verification.
     */
    @JvmField
    val DATABASE_SSL_CA_CERT = SettingSpecificationBuilder(SettingKey.Companion.ofString("database.ssl.ca.cert"))
        .setDescription(
            SettingDescription.Companion.text(
                "Certificate Authority (CA) certificate for server verification with flexible input support. " +
                        "Supports both PEM certificate content and file path through automatic detection:\n\n" +
                        "- PEM content: Provide PEM formated CA certificate directly with BEGIN/END markers\n" +
                        "- File path: Provide path to PEM-formatted CA certificate file\n\n" +
                        "The system automatically detects the input format based on PEM headers. " +
                        "Required when using custom or self-signed certificates for server verification."
            )
        )
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue("")
        .setRequired(false)
        .setAllowAnyValue(true)
        .build()

    /**
     * Enable server certificate verification for enhanced security.
     *
     * When enabled, validates that the server's SSL certificate is trusted and valid.
     * Disable only for testing with self-signed certificates or in development environments.
     */
    @JvmField
    val DATABASE_SSL_VERIFY_SERVER = SettingSpecificationBuilder(SettingKey.Companion.ofBoolean("database.ssl.verify.server"))
        .setDescription(
            SettingDescription.Companion.text(
                "Enable server certificate verification. Validates that the server's SSL " +
                        "certificate is trusted and valid. Disable only for testing or development environments."
            )
        )
        .setSupportedSources(LOCAL_SOURCE)
        .setDefaultValue(true)
        .setRequired(false)
        .build()

    /**
     * Allow connections to servers with all SSL certificates.
     *
     * Bypasses certificate chain validation to accept all certificates.
     * Should only be enabled in development or testing environments due to
     * security implications.
     */
    @JvmField
    val DATABASE_SSL_ALLOW_ALL =
        SettingSpecificationBuilder(SettingKey.Companion.ofBoolean("database.ssl.allow-all"))
            .setDescription(
                SettingDescription.Companion.text(
                    "Allow all SSL certificates. Bypasses certificate chain validation " +
                            "to accept self-signed certificates. Should only be enabled in development or testing environments."
                )
            )
            .setSupportedSources(LOCAL_SOURCE)
            .setDefaultValue(false)
            .setRequired(false)
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
    val DATABASE_POOL_MAX_SIZE = SettingSpecificationBuilder(SettingKey.Companion.ofInt("database.pool.max.size"))
        .setDescription(
            SettingDescription.Companion.text(
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
    val DATABASE_POOL_MIN_IDLE = SettingSpecificationBuilder(SettingKey.Companion.ofInt("database.pool.min.idle"))
        .setDescription(
            SettingDescription.Companion.text(
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
        SettingSpecificationBuilder(SettingKey.Companion.ofLong("database.pool.connection.timeout"))
            .setDescription(
                SettingDescription.Companion.text(
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
    val DATABASE_POOL_MAX_LIFETIME = SettingSpecificationBuilder(SettingKey.Companion.ofLong("database.pool.max.lifetime"))
        .setDescription(
            SettingDescription.Companion.text(
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
    val DATABASE_POOL_IDLE_TIMEOUT = SettingSpecificationBuilder(SettingKey.Companion.ofLong("database.pool.idle.timeout"))
        .setDescription(
            SettingDescription.Companion.text(
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
        SettingSpecificationBuilder(SettingKey.Companion.ofLong("database.pool.leak.detection.threshold"))
            .setDescription(
                SettingDescription.Companion.text(
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
        DATABASE_SSL_MODE,
        DATABASE_SSL_CLIENT_CERT,
        DATABASE_SSL_CLIENT_KEY,
        DATABASE_SSL_CA_CERT,
        DATABASE_SSL_VERIFY_SERVER,
        DATABASE_SSL_ALLOW_ALL,
        DATABASE_POOL_MAX_SIZE,
        DATABASE_POOL_MIN_IDLE,
        DATABASE_POOL_CONNECTION_TIMEOUT,
        DATABASE_POOL_MAX_LIFETIME,
        DATABASE_POOL_IDLE_TIMEOUT,
        DATABASE_POOL_LEAK_DETECTION_THRESHOLD
    )
}