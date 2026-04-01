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
import tech.lamprism.lampray.system.database.DatabaseType
import tech.lamprism.lampray.system.database.DatabaseUrl
import tech.lamprism.lampray.system.database.DatabaseUrlBuilder
import tech.lamprism.lampray.system.database.addResourceCleanupSuppressed

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
        val sslArtifacts = if (config.ssl.isEnabled()) {
            buildSslArtifacts(config)
        } else {
            DatabaseSslArtifacts.EMPTY
        }
        val parameters = try {
            buildAdditionalProperties(config, sslArtifacts.properties)
        } catch (e: Exception) {
            addResourceCleanupSuppressed(sslArtifacts.resources, e)
            throw e
        }

        return DatabaseUrl(baseUrl, parameters, sslArtifacts.resources)
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
    protected open fun buildAdditionalProperties(
        config: DatabaseConfig,
        managedSslProperties: Map<String, String>
    ): Map<String, String> {
        val params = linkedMapOf<String, String>()

        config.charset?.let { charset ->
            if (charset.isNotBlank()) {
                addCharsetParameter(params, charset)
            }
        }

        params.putAll(managedSslProperties)
        mergeCustomOptions(params, config)

        return params
    }

    protected open fun buildSslArtifacts(config: DatabaseConfig): DatabaseSslArtifacts {
        return DatabaseSslArtifacts(buildSslProperties(config))
    }

    protected open fun buildSslProperties(config: DatabaseConfig): Map<String, String> = emptyMap()

    protected open fun getReservedSslOptionKeys(): Set<String> = emptySet()

    private fun mergeCustomOptions(params: MutableMap<String, String>, config: DatabaseConfig) {
        if (config.customOptions.isEmpty()) {
            return
        }

        val customOptions = parseCustomOptions(config.customOptions)
        if (config.ssl.isEnabled()) {
            val reservedKeys = getReservedSslOptionKeys()
                .map { it.lowercase() }
                .toSet()
            val conflictingKeys = customOptions.keys.filter { key ->
                reservedKeys.contains(key.lowercase())
            }
            require(conflictingKeys.isEmpty()) {
                "Managed database SSL options conflict with database.options keys: ${conflictingKeys.joinToString(", ")}. " +
                        "Use database.ssl.mode for SSL behavior and keep database.options for supplemental driver properties only."
            }
        }

        params.putAll(customOptions)
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

        val properties = linkedMapOf<String, String>()
        options.mapNotNull { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) {
                parts[0].trim() to parts[1].trim()
            } else null
        }.forEach { (key, value) ->
            properties[key] = value
        }

        return properties
    }
}
