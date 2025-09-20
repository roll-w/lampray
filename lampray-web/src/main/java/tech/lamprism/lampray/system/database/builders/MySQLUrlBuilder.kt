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
import tech.lamprism.lampray.system.database.DatabaseType

/**
 * URL builder for MySQL and MariaDB databases.
 * Supports both MySQL and MariaDB with similar connection parameters.
 *
 * @author RollW
 */
class MySQLUrlBuilder : AbstractDatabaseUrlBuilder() {

    override val supportedTypes = setOf(DatabaseType.MYSQL, DatabaseType.MARIADB)

    override fun buildBaseUrl(config: DatabaseConfig): String {
        val target = config.target
        return if (target.isNetwork()) {
            val database = config.databaseName.ifBlank {
                throw IllegalArgumentException("Database name must be specified for MySQL or MariaDB")
            }
            "${config.type.urlPrefix}${target.getNetworkAddress()}/$database"
        } else {
            throw IllegalArgumentException("MySQL requires network target format (host:port or host)")
        }
    }

    override fun addCharsetParameter(params: MutableMap<String, String>, charset: String) {
        // MySQL uses characterEncoding parameter
        params["characterEncoding"] = charset
        // Also set useUnicode for proper Unicode support
        params["useUnicode"] = "true"
    }

    override fun getDefaultValidationQuery(): String = "SELECT 1"

    override fun validateConfig(config: DatabaseConfig) {
        super.validateConfig(config)

        // MySQL-specific validations
        require(config.target.isNetwork()) {
            "MySQL requires network target format (host:port or host), got: ${config.target}"
        }
    }
}
