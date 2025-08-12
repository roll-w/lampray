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

import tech.lamprism.lampray.system.database.DatabaseUrlBuilder

/**
 * Registry for all database URL builders.
 * Provides centralized access to all available builders.
 *
 * @author RollW
 */
object DatabaseUrlBuilders {

    private val builders: List<DatabaseUrlBuilder> = listOf(
        MySQLUrlBuilder(),
        PostgreSQLUrlBuilder(),
        SQLiteUrlBuilder(),
        H2UrlBuilder(),
        SQLServerUrlBuilder(),
        OracleUrlBuilder()
    )

    /**
     * Gets all registered database URL builders.
     *
     * @return List of all available builders
     */
    fun getAllBuilders(): List<DatabaseUrlBuilder> = builders
}
