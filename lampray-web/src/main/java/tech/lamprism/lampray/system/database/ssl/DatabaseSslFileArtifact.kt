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
 * Represents a materialized SSL file artifact.
 *
 * For file-based material, the path references the original file.
 * For inline material, the path references a temporary file that will be
 * cleaned up via [resources].
 *
 * @param path the path to the certificate/key file
 * @param resources resources to clean up (empty for file-based material)
 *
 * @author RollW
 */
data class DatabaseSslFileArtifact(
    val path: Path,
    val resources: List<AutoCloseable> = emptyList()
)
