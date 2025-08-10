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

import tech.lamprism.lampray.web.configuration.database.DatabaseConfig
import tech.lamprism.lampray.web.configuration.database.DatabaseTarget
import tech.lamprism.lampray.web.configuration.database.DatabaseType
import tech.lamprism.lampray.web.configuration.database.DatabaseUrl
import tech.lamprism.lampray.web.configuration.database.DatabaseUrlBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Abstract base class for database URL builders providing common functionality.
 *
 * @author RollW
 */
abstract class AbstractDatabaseUrlBuilder : DatabaseUrlBuilder {

    protected abstract val supportedTypes: Set<DatabaseType>

    override fun supports(type: DatabaseType): Boolean = supportedTypes.contains(type)

    override fun buildUrl(config: DatabaseConfig): DatabaseUrl {
        validateConfig(config)

        val baseUrl = buildBaseUrl(config)
        val parameters = buildAdditionalProperties(config)

        return DatabaseUrl(baseUrl, parameters)
    }

    /**
     * Validates the database configuration for this specific database type.
     *
     * @param config Database configuration to validate
     * @throws IllegalArgumentException if the configuration is invalid
     */
    open fun validateConfig(config: DatabaseConfig) {
        require(supports(config.type)) {
            "Configuration type ${config.type.typeName} is not supported by this builder"
        }
    }

    /**
     * Builds the base JDBC URL without parameters.
     *
     * @param config Database configuration
     * @return Base URL string
     */
    protected abstract fun buildBaseUrl(config: DatabaseConfig): String

    /**
     * Builds additional properties for the JDBC driver.
     *
     * @param config Database configuration
     * @return Map of parameter key-value pairs
     */
    protected open fun buildAdditionalProperties(config: DatabaseConfig): Map<String, String> {
        val params = mutableMapOf<String, String>()

        // Add charset if specified
        config.charset?.let { charset ->
            addCharsetParameter(params, charset)
        }

        // Add SSL parameters if enabled
        if (config.sslConfig.isEnabled()) {
            addSslParameters(params, config)
        }

        // Add custom options
        if (config.customOptions.isNotEmpty()) {
            parseCustomOptions(config.customOptions).forEach { (key, value) ->
                params[key] = value
            }
        }

        return params
    }

    /**
     * Adds charset parameter to the parameters map.
     * Different databases use different parameter names for charset.
     *
     * @param params Parameters map to add to
     * @param charset Character set value
     */
    protected abstract fun addCharsetParameter(params: MutableMap<String, String>, charset: String)

    /**
     * Adds SSL-related parameters to the parameters map.
     * Different databases have different SSL parameter formats.
     *
     * @param params Parameters map to add to
     * @param config Database configuration containing SSL settings
     */
    protected abstract fun addSslParameters(params: MutableMap<String, String>, config: DatabaseConfig)

    /**
     * Gets the default validation query for this database type.
     *
     * @return SQL query string for connection validation
     */
    abstract override fun getDefaultValidationQuery(): String

    /**
     * Parses custom options string into key-value pairs.
     * Format: ["key1=value1", "key2=value2"]
     */
    private fun parseCustomOptions(options: Collection<String>): Map<String, String> {
        if (options.isEmpty()) {
            return emptyMap()
        }

        return options.mapNotNull { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) {
                parts[0].trim() to parts[1].trim()
            } else null
        }.toMap()
    }
}
