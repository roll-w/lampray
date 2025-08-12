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

import java.io.File

/**
 * Represents a certificate value that can be either a direct certificate content or a file path.
 *
 * @author RollW
 */
data class CertificateValue(
    val type: CertificateType,
    val value: String
) {
    enum class CertificateType {
        /**
         * Direct certificate content (PEM format)
         */
        VALUE,

        /**
         * File path to certificate
         */
        PATH
    }

    companion object {
        /**
         * Creates a CertificateValue from pem value
         */
        fun fromValue(content: String): CertificateValue {
            return CertificateValue(CertificateType.VALUE, content)
        }

        /**
         * Creates a CertificateValue from file path
         */
        fun fromPath(path: String): CertificateValue {
            return CertificateValue(CertificateType.PATH, path)
        }
    }

    /**
     * Gets the certificate content as string.
     * If this is a file path, reads the file content.
     */
    fun getContent(): String {
        return when (type) {
            CertificateType.VALUE -> value
            CertificateType.PATH -> {
                try {
                    File(value).readText()
                } catch (e: Exception) {
                    throw IllegalArgumentException("Failed to read certificate file: $value", e)
                }
            }
        }
    }

    /**
     * Checks if this certificate value is empty or blank
     */
    fun isEmpty(): Boolean = value.isBlank()
}
