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

package tech.lamprism.lampray.web.configuration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement
import tech.lamprism.lampray.setting.ConfigProvider
import tech.lamprism.lampray.web.ServerInitializeException
import tech.lamprism.lampray.web.common.keys.DatabaseConfigKeys
import tech.lamprism.lampray.web.configuration.database.CertificateValue
import tech.lamprism.lampray.web.configuration.database.ConnectionPoolConfig
import tech.lamprism.lampray.web.configuration.database.DatabaseConfig
import tech.lamprism.lampray.web.configuration.database.DatabaseType
import tech.lamprism.lampray.web.configuration.database.DatabaseUrlBuilderFactory
import tech.lamprism.lampray.web.configuration.database.SslConfig
import tech.lamprism.lampray.web.configuration.database.ssl.SslCertificateUtils
import javax.sql.DataSource

/**
 * Data source configuration with HikariCP connection pool support for multiple database types.
 * Uses strategy pattern for clean and maintainable JDBC URL construction.
 * Integrates SSL/TLS certificate management and optimized connection pool configuration.
 *
 * @author RollW
 */
@Configuration
@EnableJpaRepositories(value = ["tech.lamprism.lampray"])
@EntityScan(value = ["tech.lamprism.lampray"])
@EnableTransactionManagement
class DataSourceConfiguration(
    @Qualifier(LocalConfigConfiguration.LOCAL_CONFIG_PROVIDER)
    private val configProvider: ConfigProvider
) {

    @Bean
    @Primary
    fun dataSourceProperties(): DataSourceProperties = DataSourceProperties().apply {
        username = configProvider[DatabaseConfigKeys.DATABASE_USERNAME]
        password = configProvider[DatabaseConfigKeys.DATABASE_PASSWORD]

        // Build URL using simplified target configuration
        val databaseConfig = databaseConfig()
        url = DatabaseUrlBuilderFactory.buildUrl(databaseConfig)
    }

    @Bean
    @Primary
    fun dataSource(databaseConfig: DatabaseConfig, dataSourceProperties: DataSourceProperties): DataSource {

        return HikariDataSource(HikariConfig().apply {
            // Basic connection properties
            jdbcUrl = dataSourceProperties.url
            username = dataSourceProperties.username
            password = dataSourceProperties.password

            // Apply HikariCP connection pool configuration
            configureHikariCP(this, databaseConfig.connectionPoolConfig, databaseConfig.type)

            // Apply SSL configuration if enabled
            if (databaseConfig.sslConfig.enabled) {
                configureHikariSsl(this, databaseConfig.sslConfig)
            }
        })
    }

    /**
     * Builds DatabaseConfig from configuration provider settings.
     */
    @Bean
    fun databaseConfig(): DatabaseConfig {
        val databaseType = DatabaseType.fromString(
            configProvider[DatabaseConfigKeys.DATABASE_TYPE] ?: throw ServerInitializeException(
                ServerInitializeException.Detail(
                    "Database type configuration is missing.",
                    "Please check the `database.type` config in the configuration file or environment variables," +
                            " and ensure it is set correctly."
                )
            )
        )

        return DatabaseConfig(
            target = configProvider[DatabaseConfigKeys.DATABASE_TARGET]
                ?: throw ServerInitializeException(ServerInitializeException.Detail(
                    "Database target configuration is missing",
                    "Please set the 'database.target' property in your configuration file"
                )),
            type = databaseType,
            databaseName = configProvider[DatabaseConfigKeys.DATABASE_NAME] ?: "",
            charset = configProvider[DatabaseConfigKeys.DATABASE_CHARSET],
            customOptions = configProvider[DatabaseConfigKeys.DATABASE_OPTIONS] ?: "",
            sslConfig = buildSslConfig(),
            connectionPoolConfig = buildConnectionPoolConfig()
        )
    }

    /**
     * Builds SSL configuration from configuration provider settings.
     */
    private fun buildSslConfig(): SslConfig {
        val enabled = configProvider[DatabaseConfigKeys.DATABASE_SSL_ENABLED] ?: false
        if (!enabled) {
            return SslConfig(enabled = false)
        }

        val mode = when (configProvider[DatabaseConfigKeys.DATABASE_SSL_MODE]?.lowercase()) {
            "disable" -> SslConfig.SslMode.DISABLE
            "prefer" -> SslConfig.SslMode.PREFER
            "require" -> SslConfig.SslMode.REQUIRE
            "verify-ca" -> SslConfig.SslMode.VERIFY_CA
            "verify-identity" -> SslConfig.SslMode.VERIFY_IDENTITY
            else -> SslConfig.SslMode.PREFER
        }

        val clientCert = createCertificateValue(
            configProvider[DatabaseConfigKeys.DATABASE_SSL_CLIENT_CERT],
            configProvider[DatabaseConfigKeys.DATABASE_SSL_CLIENT_CERT_PATH]
        )

        val clientKey = createCertificateValue(
            configProvider[DatabaseConfigKeys.DATABASE_SSL_CLIENT_KEY],
            configProvider[DatabaseConfigKeys.DATABASE_SSL_CLIENT_KEY_PATH]
        )

        val caCert = createCertificateValue(
            configProvider[DatabaseConfigKeys.DATABASE_SSL_CA_CERT],
            configProvider[DatabaseConfigKeys.DATABASE_SSL_CA_CERT_PATH]
        )

        return SslConfig(
            enabled = true,
            mode = mode,
            clientCertificate = clientCert,
            clientPrivateKey = clientKey,
            caCertificate = caCert,
            verifyServerCertificate = configProvider[DatabaseConfigKeys.DATABASE_SSL_VERIFY_SERVER] ?: true,
            allowSelfSignedCertificates = configProvider[DatabaseConfigKeys.DATABASE_SSL_ALLOW_SELF_SIGNED] ?: false
        )
    }

    /**
     * Builds connection pool configuration from configuration provider settings.
     */
    private fun buildConnectionPoolConfig(): ConnectionPoolConfig {
        return ConnectionPoolConfig(
            initialSize = 0, // HikariCP doesn't use initial size
            maxActive = configProvider[DatabaseConfigKeys.DATABASE_POOL_MAX_SIZE] ?: 20,
            maxIdle = 0, // HikariCP doesn't use max idle
            minIdle = configProvider[DatabaseConfigKeys.DATABASE_POOL_MIN_IDLE] ?: 2,
            maxWait = 0, // HikariCP doesn't use max wait
            connectionTimeout = configProvider[DatabaseConfigKeys.DATABASE_POOL_CONNECTION_TIMEOUT] ?: 30000L,
            testOnBorrow = true, // HikariCP always validates
            testOnReturn = false,
            testWhileIdle = true,
            timeBetweenEvictionRuns = configProvider[DatabaseConfigKeys.DATABASE_POOL_IDLE_TIMEOUT] ?: 600000L,
            minEvictableIdleTime = configProvider[DatabaseConfigKeys.DATABASE_POOL_MAX_LIFETIME] ?: 1800000L,
            removeAbandoned = false, // HikariCP doesn't use this concept
            removeAbandonedTimeout = 0,
            logAbandoned = (configProvider[DatabaseConfigKeys.DATABASE_POOL_LEAK_DETECTION_THRESHOLD] ?: 0L) > 0
        )
    }

    /**
     * Creates CertificateValue from content or path, preferring content over path.
     */
    private fun createCertificateValue(content: String?, path: String?): CertificateValue? {
        return when {
            !content.isNullOrBlank() -> CertificateValue.fromContent(content)
            !path.isNullOrBlank() -> CertificateValue.fromPath(path)
            else -> null
        }
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
            maxLifetime = poolConfig.minEvictableIdleTime
            idleTimeout = poolConfig.timeBetweenEvictionRuns

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
                "Failed to configure database connection pool.",
                "Please check the connection pool and database configuration settings in the configuration file or environment variables." +
                        " Ensure that the values are valid and accessible."
            ), e
        )
    }

    /**
     * Configures SSL settings for HikariCP DataSource.
     * Note: SSL certificates are applied through HikariCP data source properties.
     */
    private fun configureHikariSsl(hikariConfig: HikariConfig, sslConfig: SslConfig) {
        if (!sslConfig.enabled) return

        try {
            // Create SSL context if certificates are provided
            if (sslConfig.clientCertificate != null || sslConfig.caCertificate != null) {
                val sslContext = SslCertificateUtils.createSslContext(
                    clientCert = sslConfig.clientCertificate,
                    clientKey = sslConfig.clientPrivateKey,
                    caCert = sslConfig.caCertificate,
                    allowSelfSigned = sslConfig.allowSelfSignedCertificates
                )

                // Apply SSL context to HikariCP data source properties
                // This approach works with most JDBC drivers
                hikariConfig.addDataSourceProperty("sslContext", sslContext)
                hikariConfig.addDataSourceProperty("useSSL", "true")

                if (!sslConfig.verifyServerCertificate) {
                    hikariConfig.addDataSourceProperty("trustServerCertificate", "true")
                    hikariConfig.addDataSourceProperty("verifyServerCertificate", "false")
                }
            }
        } catch (e: Exception) {
            throw ServerInitializeException(
                ServerInitializeException.Detail(
                    "Failed to configure SSL for data source.",
                    "Please check the SSL configuration settings in the configuration file or environment variables." +
                            " Ensure that the certificates are valid and accessible."
                ), e
            )
        }
    }
}