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

import tech.lamprism.lampray.system.database.builders.H2UrlBuilder
import tech.lamprism.lampray.system.database.builders.MySQLUrlBuilder
import tech.lamprism.lampray.system.database.builders.OracleUrlBuilder
import tech.lamprism.lampray.system.database.builders.PostgreSQLUrlBuilder
import tech.lamprism.lampray.system.database.builders.SQLServerUrlBuilder
import tech.lamprism.lampray.system.database.builders.SQLiteUrlBuilder

/**
 * Factory for creating and managing database URL builders.
 * Provides a centralized way to build JDBC URLs for different database types.
 *
 * @author RollW
 */
object DatabaseUrlBuilderFactory {

    private val builders: List<DatabaseUrlBuilder> = listOf(
        MySQLUrlBuilder(),
        PostgreSQLUrlBuilder(),
        SQLiteUrlBuilder(),
        H2UrlBuilder(),
        SQLServerUrlBuilder(),
        OracleUrlBuilder()
    )

    /**
     * Builds a JDBC URL for the given database configuration.
     *
     * @param config Database configuration
     * @return Complete JDBC URL string
     * @throws IllegalArgumentException if no builder supports the database type
     */
    fun buildUrl(config: DatabaseConfig): DatabaseUrl {
        val builder = getBuilder(config.type)
        return builder.buildUrl(config)
    }

    /**
     * Gets the appropriate URL builder for the specified database type.
     *
     * @param type Database type
     * @return DatabaseUrlBuilder for the specified type
     * @throws IllegalArgumentException if no builder supports the database type
     */
    fun getBuilder(type: DatabaseType): DatabaseUrlBuilder {
        return builders.find { it.supports(type) }
            ?: throw IllegalArgumentException("No URL builder found for database type: ${type.typeName}")
    }
}
