package tech.lamprism.lampray.system.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

class ManagedHikariDataSource(
    configuration: HikariConfig,
    private val databaseUrl: DatabaseUrl
) : HikariDataSource(configuration) {
    override fun close() {
        try {
            super.close()
        } finally {
            databaseUrl.closeResources()
        }
    }
}
