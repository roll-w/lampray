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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

/**
 * Handles temporary file operations for database SSL material with secure permissions.
 *
 * Creates temporary files with owner-only read/write permissions (where supported)
 * and provides cleanup utilities.
 *
 * @author RollW
 */
internal class DatabaseSslFileHandler {

    private val logger: Logger = LoggerFactory.getLogger(DatabaseSslFileHandler::class.java)

    /**
     * Creates a temporary file with the given prefix and suffix.
     * The file will have owner-only permissions on POSIX systems.
     *
     * @param prefix the filename prefix
     * @param suffix the filename suffix (e.g., ".pem", ".p12")
     * @return the path to the created temporary file
     */
    fun createTempFile(prefix: String, suffix: String): Path {
        val path = Files.createTempFile(TEMP_DIR_PREFIX + prefix, suffix)
        setOwnerOnlyPermissions(path)
        return path
    }

    /**
     * Sets owner-only read/write permissions on the specified file.
     * Falls back to File API if POSIX permissions are not supported.
     */
    private fun setOwnerOnlyPermissions(path: Path) {
        try {
            val permissions = setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            )
            Files.setPosixFilePermissions(path, permissions)
        } catch (e: UnsupportedOperationException) {
            logger.debug("POSIX permissions not supported, falling back to File API for: {}", path)
            trySetOwnerOnlyPermissionsWithFileApi(path)
        } catch (e: IOException) {
            logger.warn("Failed to restrict file permissions due to IO error: {}", path, e)
        } catch (e: SecurityException) {
            logger.warn("Failed to restrict file permissions due to security policy: {}", path, e)
        }
    }

    private fun trySetOwnerOnlyPermissionsWithFileApi(path: Path) {
        try {
            val file = path.toFile()
            // Remove all group/other permissions
            file.setReadable(false, false)
            file.setWritable(false, false)
            file.setExecutable(false, false)
            // Set owner permissions
            val ownerRead = file.setReadable(true, true)
            val ownerWrite = file.setWritable(true, true)
            if (!ownerRead || !ownerWrite) {
                logger.warn(
                    "Failed to restrict database SSL temporary file permissions with the file API: {}",
                    path
                )
            }
        } catch (e: SecurityException) {
            logger.warn("Failed to restrict file permissions with File API: {}", path, e)
        }
    }

    companion object {
        private const val TEMP_DIR_PREFIX = "lampray-db-"
    }
}
