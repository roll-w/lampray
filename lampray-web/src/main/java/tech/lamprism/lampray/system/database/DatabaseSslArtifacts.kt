package tech.lamprism.lampray.system.database

import java.nio.file.Path

data class DatabaseSslArtifacts(
    val properties: Map<String, String> = emptyMap(),
    val resources: List<AutoCloseable> = emptyList()
) {
    companion object {
        val EMPTY = DatabaseSslArtifacts()
    }
}

data class DatabaseSslFileArtifact(
    val path: Path,
    val resources: List<AutoCloseable> = emptyList()
)

data class DatabaseSslKeyStoreArtifact(
    val path: Path,
    val password: String,
    val type: String,
    val resources: List<AutoCloseable> = emptyList()
)
