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

package tech.lamprism.lampray.web.configuration.database

/**
 * Represents a parsed database connection target with type classification.
 * Provides methods to determine the target type and extract connection details.
 *
 * @author RollW
 */
data class DatabaseTarget(
    val originalValue: String,
    val type: TargetType,
    val host: String? = null,
    val port: Int? = null,
    val path: String? = null,
    val databaseName: String? = null
) {

    enum class TargetType {
        /**
         * In-memory database (e.g., "memory")
         */
        MEMORY,

        /**
         * File-based database (e.g., "file:./data/app.db", "file:/path/to/db.sqlite")
         */
        FILE,

        /**
         * Network-based database (e.g., "localhost:3306", "db.example.com")
         */
        NETWORK
    }

    /**
     * Checks if this target represents an in-memory database.
     */
    fun isMemory(): Boolean = type == TargetType.MEMORY

    /**
     * Checks if this target represents a file-based database.
     */
    fun isFile(): Boolean = type == TargetType.FILE

    /**
     * Checks if this target represents a network-based database.
     */
    fun isNetwork(): Boolean = type == TargetType.NETWORK

    /**
     * Gets the network address as host:port string.
     * Returns null if this is not a network target.
     */
    fun getNetworkAddress(): String? {
        return if (isNetwork() && host != null && port != null) {
            "$host:$port"
        } else null
    }

    /**
     * Gets the effective file path for file-based targets.
     * Returns null if this is not a file target.
     */
    fun getFilePath(): String? {
        return if (isFile()) path else null
    }

    companion object {
        /**
         * Parses a target string and returns a DatabaseTarget instance.
         *
         * @param target The target string to parse
         * @param defaultPort Default port to use if not specified in network targets
         * @return Parsed DatabaseTarget
         */
        fun parse(target: String, defaultPort: Int = 0): DatabaseTarget {
            if (target.isBlank()) {
                throw IllegalArgumentException("Target cannot be empty")
            }
            val trimmedTarget = target.trim()

            return when {
                // Memory targets - only "memory" is allowed
                trimmedTarget.equals("memory", ignoreCase = true) -> {
                    DatabaseTarget(
                        originalValue = target,
                        type = TargetType.MEMORY
                    )
                }

                // File targets - must use "file:" prefix
                trimmedTarget.startsWith("file:", ignoreCase = true) -> {
                    val filePath = trimmedTarget.substring(5) // Remove "file:" prefix
                    if (filePath.isBlank()) {
                        throw IllegalArgumentException("File path cannot be empty after 'file:' prefix in target: $target")
                    }
                    DatabaseTarget(
                        originalValue = target,
                        type = TargetType.FILE,
                        path = filePath
                    )
                }

                // Network targets - everything else (no validation)
                else -> {
                    parseNetworkTarget(trimmedTarget, defaultPort, target)
                }
            }
        }

        /**
         * Parses network target into host and port components.
         * No validation is performed on hostname or IP format.
         */
        private fun parseNetworkTarget(target: String, defaultPort: Int, originalTarget: String): DatabaseTarget {
            val parts = target.split(":")

            return when (parts.size) {
                1 -> {
                    // Host only, use default port
                    DatabaseTarget(
                        originalValue = originalTarget,
                        type = TargetType.NETWORK,
                        host = parts[0],
                        port = defaultPort
                    )
                }
                2 -> {
                    // Host:port format
                    val port = parts[1].toIntOrNull()
                    if (port == null || port !in 1..65535) {
                        throw IllegalArgumentException("Invalid port number in target: $originalTarget. Port must be between 1 and 65535.")
                    }
                    DatabaseTarget(
                        originalValue = originalTarget,
                        type = TargetType.NETWORK,
                        host = parts[0],
                        port = port
                    )
                }
                else -> {
                    throw IllegalArgumentException("Invalid network target format: $originalTarget. Expected format: 'hostname' or 'hostname:port'")
                }
            }
        }
    }
}
