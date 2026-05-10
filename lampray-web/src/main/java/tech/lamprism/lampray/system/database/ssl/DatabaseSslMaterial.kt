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

data class DatabaseSslMaterial(
    val source: DatabaseSslMaterialSource,
    val value: String
) {
    companion object {
        fun parse(rawValue: String, keyName: String): DatabaseSslMaterial {
            val separatorIndex = rawValue.indexOf(':')
            require(separatorIndex > 0) {
                "Invalid $keyName format. Use 'file:/path/to/file.pem' or 'value:<pem-content>'."
            }

            val source = rawValue.substring(0, separatorIndex).trim().lowercase()
            val value = rawValue.substring(separatorIndex + 1).trim()
            require(value.isNotEmpty()) {
                "$keyName cannot be empty."
            }

            return when (source) {
                "file" -> DatabaseSslMaterial(DatabaseSslMaterialSource.FILE, value)
                "value" -> DatabaseSslMaterial(DatabaseSslMaterialSource.VALUE, value)
                else -> throw IllegalArgumentException(
                    "Unsupported $keyName source '$source'. Supported sources: file, value."
                )
            }
        }
    }
}

enum class DatabaseSslMaterialSource {
    FILE,
    VALUE
}
