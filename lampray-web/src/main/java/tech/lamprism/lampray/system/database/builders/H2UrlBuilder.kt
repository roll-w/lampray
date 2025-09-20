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
import java.io.File

/**
 * URL builder for H2 databases.
 * Supports file-based, memory, and TCP server modes of H2 database.
 *
 * @author RollW
 */
class H2UrlBuilder : AbstractDatabaseUrlBuilder() {

    override val supportedTypes = setOf(DatabaseType.H2)

    override fun buildBaseUrl(config: DatabaseConfig): String {
        val target = config.target

        return when {
            target.isMemory() -> {
                "jdbc:h2:mem:lampray;DB_CLOSE_DELAY=-1"
            }

            target.isNetwork() -> {
                // TCP server mode
                val database = config.databaseName.ifBlank {
                    throw IllegalArgumentException("Database name must be specified for H2 TCP server mode")
                }
                "jdbc:h2:tcp://${target.getNetworkAddress()}/$database"
            }

            target.isFile() -> {
                // File-based database
                val filePath = target.getFilePath()!!
                val file = File(filePath)
                val absolutePath = file.absolutePath
                // Remove .db extension if present, H2 adds it automatically
                val dbPath = absolutePath.removeSuffix(".db")

                "jdbc:h2:file:$dbPath"
            }

            else -> {
                throw IllegalArgumentException("Unsupported database type ${target.type}")
            }
        }

    }

    override fun addCharsetParameter(params: MutableMap<String, String>, charset: String) {
        // We don't need to add charset for H2, it uses UTF-8 by default
        // and does not support custom charset settings in the URL.
    }

    override fun getDefaultValidationQuery(): String = "SELECT 1"

    override fun validateConfig(config: DatabaseConfig) {
        super.validateConfig(config)

        val target = config.target

        // For file-based databases, ensure parent directory exists
        if (target.isFile()) {
            try {
                val filePath = target.getFilePath()!!
                val file = File(filePath)
                val parentDir = file.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs()
                }
            } catch (e: Exception) {
                throw IllegalArgumentException("Cannot create directory for H2 database: ${config.target}", e)
            }
        }
    }
}
