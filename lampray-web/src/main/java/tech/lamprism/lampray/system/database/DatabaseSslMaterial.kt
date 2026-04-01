package tech.lamprism.lampray.system.database

data class DatabaseSslMaterial(
    val source: DatabaseSslMaterialSource,
    val value: String
) {
    companion object {
        fun parse(rawValue: String, keyName: String): DatabaseSslMaterial {
            val separatorIndex = rawValue.indexOf(':')
            require(separatorIndex > 0) {
                "Invalid $keyName format. Use 'file:/path/to/file.pem' or 'value:<pem-content>'."
            }

            val source = rawValue.substring(0, separatorIndex).trim().lowercase()
            val value = rawValue.substring(separatorIndex + 1).trim()
            require(value.isNotEmpty()) {
                "$keyName cannot be empty."
            }

            return when (source) {
                "file" -> DatabaseSslMaterial(DatabaseSslMaterialSource.FILE, value)
                "value" -> DatabaseSslMaterial(DatabaseSslMaterialSource.VALUE, value)
                else -> throw IllegalArgumentException(
                    "Unsupported $keyName source '$source'. Supported sources: file, value."
                )
            }
        }
    }
}

enum class DatabaseSslMaterialSource {
    FILE,
    VALUE
}
