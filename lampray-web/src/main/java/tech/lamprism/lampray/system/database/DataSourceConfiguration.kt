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

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement
import tech.lamprism.lampray.setting.ConfigProvider
import tech.lamprism.lampray.setting.SettingSpecification.Companion.keyName
import tech.lamprism.lampray.web.ServerInitializeException
import tech.lamprism.lampray.web.configuration.LocalConfigConfiguration
import javax.sql.DataSource

private val logger = logger<DataSourceConfiguration>()

/**
 * Data source configuration with HikariCP connection pool support for multiple database types.
 *
 * @author RollW
 */
@Configuration
@EnableJpaRepositories(value = ["tech.lamprism.lampray"])
@EntityScan(value = ["tech.lamprism.lampray"])
@EnableTransactionManagement
class DataSourceConfiguration(
    @param:Qualifier(LocalConfigConfiguration.LOCAL_CONFIG_PROVIDER)
    private val configProvider: ConfigProvider
) {

    /**
     * @throws ServerInitializeException if the database URL cannot be built
     */
    @Bean
    fun databaseUrl(databaseConfig: DatabaseConfig): DatabaseUrl = try {
        DatabaseUrlBuilderFactory.buildUrl(databaseConfig)
    } catch (e: ServerInitializeException) {
        throw e
    } catch (e: Exception) {
        throw ServerInitializeException(
            ServerInitializeException.Detail(
                "Failed to build database URL. ${e.message}",
                "Please check the database config in the configuration file or " +
                        "environment variables. Ensure that the values are valid and accessible."
            ), e
        )
    }

    @Bean
    @Primary
    fun dataSourceProperties(databaseUrl: DatabaseUrl): DataSourceProperties = DataSourceProperties().apply {
        username = configProvider[DatabaseConfigKeys.DATABASE_USERNAME]
        password = configProvider[DatabaseConfigKeys.DATABASE_PASSWORD]
        url = databaseUrl.url
        logger.info("Database URL configured: $url")
    }

    @Bean(destroyMethod = "close")
    @Primary
    fun dataSource(
        databaseConfig: DatabaseConfig,
        dataSourceProperties: DataSourceProperties,
        databaseUrl: DatabaseUrl
    ): DataSource {
        return try {
            ManagedHikariDataSource(HikariConfig().apply {
                jdbcUrl = dataSourceProperties.url
                username = dataSourceProperties.username
                password = dataSourceProperties.password
                this.dataSourceProperties.putAll(databaseUrl.properties)
                configureHikariCP(this, databaseConfig.connectionPoolConfig, databaseConfig.type)
            }, databaseUrl)
        } catch (e: Exception) {
            try {
                databaseUrl.closeResources()
            } catch (cleanupException: Exception) {
                e.addSuppressed(cleanupException)
            }
            throw e
        }
    }

    /**
     * Builds DatabaseConfig from configuration provider settings.
     */
    @Bean
    fun databaseConfig(): DatabaseConfig {
        val type = configProvider[DatabaseConfigKeys.DATABASE_TYPE] ?: throw ServerInitializeException(
            ServerInitializeException.Detail(
                "Database type configuration is missing.",
                "Please check the 'database.type' config in the configuration file or environment variables," +
                        " and ensure it is set correctly."
            )
        )
        val databaseType = try {
            DatabaseType.fromString(type)
        } catch (e: IllegalArgumentException) {
            throw ServerInitializeException(
                ServerInitializeException.Detail(
                    "Invalid database type configuration. ${e.message}",
                    "The 'database.type' config must be one of: ${
                        DatabaseType.entries.joinToString(", ") { it.typeName }
                    }, but got '$type'. Please check your configuration file or environment variables."
                ), e
            )
        }

        val target = (configProvider[DatabaseConfigKeys.DATABASE_TARGET]
            ?: throw ServerInitializeException(
                ServerInitializeException.Detail(
                    "Database target configuration is missing",
                    "Please set the 'database.target' property in your configuration file"
                )
            ))
        return DatabaseConfig(
            target = DatabaseTarget.parse(target, databaseType.defaultPort),
            type = databaseType,
            databaseName = configProvider[DatabaseConfigKeys.DATABASE_NAME]
                ?: DatabaseConfigKeys.DATABASE_NAME.defaultValue!!,
            charset = configProvider[DatabaseConfigKeys.DATABASE_CHARSET],
            ssl = buildSslConfig(databaseType),
            customOptions = configProvider[DatabaseConfigKeys.DATABASE_OPTIONS] ?: emptySet(),
            connectionPoolConfig = buildConnectionPoolConfig()
        )
    }

    private fun buildSslConfig(databaseType: DatabaseType): DatabaseSslConfig {
        val configuredSslMode = readRawConfig(DatabaseConfigKeys.DATABASE_SSL_MODE.keyName)
        val configuredCa = readRawConfig(DatabaseConfigKeys.DATABASE_SSL_CA.keyName)
        val configuredCertificate = readRawConfig(DatabaseConfigKeys.DATABASE_SSL_CERT.keyName)
        val configuredKey = readRawConfig(DatabaseConfigKeys.DATABASE_SSL_KEY.keyName)

        if (isSslUnsupportedDatabaseType(databaseType)) {
            if (hasExplicitSslConfiguration(configuredSslMode, configuredCa, configuredCertificate, configuredKey)) {
                logger.warn {
                    "Database type '${databaseType.typeName}' does not support SSL. Ignoring configured database.ssl.* settings."
                }
            }
            return DatabaseSslConfig()
        }

        val sslModeValue = configuredSslMode ?: DatabaseConfigKeys.DATABASE_SSL_MODE.defaultValue!!
        val sslMode = try {
            DatabaseSslMode.fromString(sslModeValue)
        } catch (e: IllegalArgumentException) {
            throw ServerInitializeException(
                ServerInitializeException.Detail(
                    "Invalid database SSL mode configuration. ${e.message}",
                    "The 'database.ssl.mode' config must be one of: ${
                        DatabaseSslMode.entries.joinToString(", ") { it.value }
                    }. Please check your configuration file or environment variables."
                ), e
            )
        }

        val ca = parseSslMaterial(DatabaseConfigKeys.DATABASE_SSL_CA)
        val certificate = parseSslMaterial(DatabaseConfigKeys.DATABASE_SSL_CERT)
        val key = parseSslMaterial(DatabaseConfigKeys.DATABASE_SSL_KEY)

        if (sslMode == DatabaseSslMode.DISABLED && (ca != null || certificate != null || key != null)) {
            throw ServerInitializeException(
                ServerInitializeException.Detail(
                    "Database SSL material requires managed SSL mode.",
                    "Set 'database.ssl.mode' to 'required', 'verify-ca', or 'verify-identity' before providing database SSL certificate material."
                )
            )
        }

        if ((certificate == null) != (key == null)) {
            throw ServerInitializeException(
                ServerInitializeException.Detail(
                    "Database client certificate configuration is incomplete.",
                    "Both 'database.ssl.cert' and 'database.ssl.key' must be configured together."
                )
            )
        }

        if (ca != null && sslMode != DatabaseSslMode.VERIFY_CA && sslMode != DatabaseSslMode.VERIFY_IDENTITY) {
            throw ServerInitializeException(
                ServerInitializeException.Detail(
                    "Database CA certificate requires a validating SSL mode.",
                    "Use 'database.ssl.mode=verify-ca' or 'database.ssl.mode=verify-identity' when 'database.ssl.ca' is configured."
                )
            )
        }

        return DatabaseSslConfig(sslMode, ca, certificate, key)
    }

    private fun isSslUnsupportedDatabaseType(databaseType: DatabaseType): Boolean {
        return databaseType == DatabaseType.SQLITE
    }

    private fun hasExplicitSslConfiguration(
        sslMode: String?,
        ca: String?,
        certificate: String?,
        key: String?
    ): Boolean {
        return sslMode != null || ca != null || certificate != null || key != null
    }

    private fun readRawConfig(keyName: String): String? = configProvider[keyName]

    private fun parseSslMaterial(specification: tech.lamprism.lampray.setting.SettingSpecification<String, String>): DatabaseSslMaterial? {
        val rawValue = configProvider[specification] ?: return null
        return try {
            DatabaseSslMaterial.parse(rawValue, specification.keyName)
        } catch (e: IllegalArgumentException) {
            throw ServerInitializeException(
                ServerInitializeException.Detail(
                    "Invalid database SSL material configuration. ${e.message}",
                    "Please use a PEM/plain-text source like 'file:/path/to/file.pem' or 'value:<pem-content>' for ${specification.keyName}."
                ), e
            )
        }
    }

    /**
     * Builds connection pool configuration from configuration provider settings.
     */
    private fun buildConnectionPoolConfig(): ConnectionPoolConfig {
        return ConnectionPoolConfig(
            maxActive = configProvider[DatabaseConfigKeys.DATABASE_POOL_MAX_SIZE] ?: 20,
            minIdle = configProvider[DatabaseConfigKeys.DATABASE_POOL_MIN_IDLE] ?: 2,
            connectionTimeout = configProvider[DatabaseConfigKeys.DATABASE_POOL_CONNECTION_TIMEOUT] ?: 30000L,
            idleTimeout = configProvider[DatabaseConfigKeys.DATABASE_POOL_IDLE_TIMEOUT] ?: 600000L,
            maxLifetime = configProvider[DatabaseConfigKeys.DATABASE_POOL_MAX_LIFETIME] ?: 1800000L,
            logAbandoned = (configProvider[DatabaseConfigKeys.DATABASE_POOL_LEAK_DETECTION_THRESHOLD] ?: 0L) > 0
        )
    }

    /**
     * Configures HikariCP connection pool settings.
     */
    private fun configureHikariCP(
        hikariConfig: HikariConfig,
        poolConfig: ConnectionPoolConfig,
        databaseType: DatabaseType
    ) = try {
        hikariConfig.apply {
            // Pool sizing - HikariCP uses maximumPoolSize instead of maxActive
            maximumPoolSize = poolConfig.maxActive
            minimumIdle = poolConfig.minIdle

            // Timeouts (HikariCP uses milliseconds)
            connectionTimeout = poolConfig.connectionTimeout
            maxLifetime = poolConfig.maxLifetime
            idleTimeout = poolConfig.idleTimeout

            // Validation
            val validationQuery = DatabaseUrlBuilderFactory.getBuilder(databaseType).getDefaultValidationQuery()
            connectionTestQuery = validationQuery

            // HikariCP specific optimizations
            isAutoCommit = true
            isAllowPoolSuspension = false
            isIsolateInternalQueries = false
            isRegisterMbeans = false
            isReadOnly = false

            // Connection pool name for monitoring
            poolName = "LamprayHikariCP-${databaseType.typeName}"

            // Leak detection
            leakDetectionThreshold = if (poolConfig.logAbandoned) {
                configProvider[DatabaseConfigKeys.DATABASE_POOL_LEAK_DETECTION_THRESHOLD] ?: 0L
            } else {
                0
            }
        }
    } catch (e: Exception) {
        throw ServerInitializeException(
            ServerInitializeException.Detail(
                "Failed to configure database connection pool. ${e.message}",
                "Please check the connection pool and database configuration settings in the configuration file or environment variables." +
                        " Ensure that the values are valid and accessible."
            ), e
        )
    }
}
