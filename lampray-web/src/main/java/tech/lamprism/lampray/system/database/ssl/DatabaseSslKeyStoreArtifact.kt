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

import java.nio.file.Path

/**
 * Represents a materialized SSL keystore artifact (PKCS12 or JKS).
 *
 * Contains the path to the keystore file, the password to access it,
 * and resources for cleanup.
 *
 * @param path the path to the keystore file
 * @param password the keystore password
 * @param type the keystore type (e.g., "PKCS12", "JKS")
 * @param resources resources to clean up the temporary keystore file
 *
 * @author RollW
 */
data class DatabaseSslKeyStoreArtifact(
    val path: Path,
    val password: String,
    val type: String,
    val resources: List<AutoCloseable> = emptyList()
)
