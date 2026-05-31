/*
 * Copyright (C) 2023-2026 RollW
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

package tech.lamprism.lampray.system.database.ssl

/**
 * Represents SSL certificate material from a specific source.
 *
 * @param source the material source type (FILE or VALUE)
 * @param value the file path or inline content
 *
 * @author RollW
 */
data class DatabaseSslMaterial(
    val source: Source,
    val value: String
) {

    /**
     * The source type of SSL material.
     */
    enum class Source {
        /**
         * Material is stored in a file. The [value] is the file path.
         */
        FILE,

        /**
         * Material is provided inline. The [value] is the content.
         */
        VALUE
    }

    companion object {
        /**
         * Parses a raw configuration value into a [DatabaseSslMaterial].
         *
         * @param rawValue the raw configuration string
         * @param keyName the configuration key name (for error messages)
         * @return the parsed material
         * @throws IllegalArgumentException if the format is invalid
         */
        @JvmStatic
        fun parse(rawValue: String, keyName: String): DatabaseSslMaterial {
            val separatorIndex = rawValue.indexOf(':')
            require(separatorIndex > 0) {
                "Invalid $keyName format. Use 'file:/path/to/file.pem' or 'value:<pem-content>'."
            }

            val sourceName = rawValue.substring(0, separatorIndex).trim().lowercase()
            val value = rawValue.substring(separatorIndex + 1).trim()
            require(value.isNotEmpty()) {
                "$keyName cannot be empty."
            }

            val source = when (sourceName) {
                "file" -> Source.FILE
                "value" -> Source.VALUE
                else -> throw IllegalArgumentException(
                    "Unsupported $keyName source '$sourceName'. Supported sources: file, value."
                )
            }

            return DatabaseSslMaterial(source, value)
        }
    }
}
