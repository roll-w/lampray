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

data class DatabaseSslConfig(
    val mode: DatabaseSslMode = DatabaseSslMode.DISABLED,
    val ca: DatabaseSslMaterial? = null,
    val certificate: DatabaseSslMaterial? = null,
    val key: DatabaseSslMaterial? = null
) {
    fun isEnabled(): Boolean = mode != DatabaseSslMode.DISABLED

    fun hasCustomMaterial(): Boolean = ca != null || certificate != null || key != null
}

enum class DatabaseSslMode(
    val value: String
) {
    DISABLED("disabled"),
    REQUIRED("required"),
    VERIFY_CA("verify-ca"),
    VERIFY_IDENTITY("verify-identity");

    companion object {
        fun fromString(value: String): DatabaseSslMode {
            return entries.find { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException(
                    "Unsupported database SSL mode: $value. Supported values: ${entries.joinToString(", ") { it.value }}"
                )
        }
    }
}
