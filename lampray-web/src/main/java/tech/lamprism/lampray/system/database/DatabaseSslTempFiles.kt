package tech.lamprism.lampray.system.database

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
