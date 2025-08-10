package tech.lamprism.lampray.web.configuration.database

/**
 * @author RollW
 */
data class DatabaseUrl(
    val url: String,
    val properties: Map<String, Any>
)
