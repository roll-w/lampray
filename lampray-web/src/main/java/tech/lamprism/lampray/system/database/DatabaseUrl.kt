package tech.lamprism.lampray.system.database

/**
 * @author RollW
 */
data class DatabaseUrl(
    val url: String,
    val properties: Map<String, String>,
    val resources: List<AutoCloseable> = emptyList()
) {
    fun closeResources() {
        val failure = closeDatabaseResources(resources)
        if (failure != null) {
            throw IllegalStateException("Failed to release database SSL resources.", failure)
        }
    }
}
