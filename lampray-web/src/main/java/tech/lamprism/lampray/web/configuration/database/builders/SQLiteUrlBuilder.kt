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
import tech.lamprism.lampray.web.configuration.database.DatabaseType
import java.io.File

/**
 * URL builder for SQLite databases.
 * Supports file-based SQLite databases with various path formats.
 *
 * @author RollW
 */
class SQLiteUrlBuilder : AbstractDatabaseUrlBuilder() {

    override val supportedTypes = setOf(DatabaseType.SQLITE)

    override fun buildBaseUrl(config: DatabaseConfig): String {
        val target = config.target

        return when {
            target.isMemory() -> {
                "jdbc:sqlite::memory:"
            }

            target.isFile() -> {
                val filePath = target.getFilePath()!!
                val file = File(filePath)
                val absolutePath = file.absolutePath

                "jdbc:sqlite:$absolutePath"
            }

            else -> {
                throw IllegalArgumentException("SQLite doesn't support network connections, got: ${config.target}")
            }
        }
    }

    override fun addCharsetParameter(params: MutableMap<String, String>, charset: String) {
        // SQLite uses encoding parameter
        when (charset.lowercase()) {
            "utf8", "utf-8", "utf8mb4" -> params["encoding"] = "UTF-8"
            else -> params["encoding"] = charset
        }
    }

    override fun addSslParameters(params: MutableMap<String, String>, config: DatabaseConfig) {
        // SQLite doesn't support SSL as it's a file-based database
        // This method is intentionally empty
    }

    override fun getDefaultValidationQuery(): String = "SELECT 1"

    override fun validateConfig(config: DatabaseConfig) {
        super.validateConfig(config)

        // SQLite-specific validations
        val target = config.target

        // Check if it's a network target (which is invalid for SQLite)
        require(!target.isNetwork()) {
            "SQLite doesn't support network connections, got: ${config.target}"
        }

        // For file-based databases, ensure parent directory exists or can be created
        if (target.isFile()) {
            try {
                val filePath = target.getFilePath()!!
                val file = File(filePath)
                val parentDir = file.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs()
                }
            } catch (e: Exception) {
                throw IllegalArgumentException("Cannot create directory for SQLite database: ${config.target}", e)
            }
        }
    }
}
