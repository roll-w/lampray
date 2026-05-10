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

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.util.UUID

internal fun createTempFile(prefix: String, suffix: String): Path {
    val path = Files.createTempFile("lampray-db-$prefix-", suffix)
    setOwnerOnlyPermissions(path)
    return path
}

private fun setOwnerOnlyPermissions(path: Path) {
    try {
        val permissions = setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
        Files.setPosixFilePermissions(path, permissions)
    } catch (_: UnsupportedOperationException) {
    } catch (_: IOException) {
    }
}

internal fun newPassword(): String = UUID.randomUUID().toString().replace("-", "")

internal fun cleanupTempPath(path: Path, cause: Exception) {
    try {
        Files.deleteIfExists(path)
    } catch (cleanupException: Exception) {
        cause.addSuppressed(cleanupException)
    }
}

internal class TemporaryPathResource(
    private val path: Path
) : AutoCloseable {
    override fun close() {
        Files.deleteIfExists(path)
    }
}
