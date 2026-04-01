package tech.lamprism.lampray.system.database

internal fun closeDatabaseResources(resources: Iterable<AutoCloseable>): Exception? {
    var failure: Exception? = null
    resources.toList().asReversed().forEach { resource ->
        try {
            resource.close()
        } catch (e: Exception) {
            if (failure == null) {
                failure = e
            } else {
                failure?.addSuppressed(e)
            }
        }
    }
    return failure
}

internal fun addResourceCleanupSuppressed(resources: Iterable<AutoCloseable>, cause: Exception) {
    closeDatabaseResources(resources)?.let(cause::addSuppressed)
}
