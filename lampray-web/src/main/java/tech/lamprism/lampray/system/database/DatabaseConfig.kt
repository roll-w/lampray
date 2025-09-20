/*
 * Copyright (C) 2023-2025 RollW
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

package tech.lamprism.lampray.system.database

/**
 * Database connection configuration data class.
 *
 * @author RollW
 */
data class DatabaseConfig(
    val target: DatabaseTarget,
    val databaseName: String,
    val type: DatabaseType,
    val charset: String?,
    val customOptions: Collection<String>,
    val connectionPoolConfig: ConnectionPoolConfig
)

/**
 * Supported database types with simplified classification.
 */
enum class DatabaseType(
    val typeName: String,
    val defaultPort: Int,
    val urlPrefix: String
) {
    MYSQL("mysql", 3306, "jdbc:mysql://"),
    MARIADB("mariadb", 3306, "jdbc:mariadb://"),
    POSTGRESQL("postgresql", 5432, "jdbc:postgresql://"),
    SQL_SERVER("sqlserver", 1433, "jdbc:sqlserver://"),
    ORACLE("oracle", 1521, "jdbc:oracle:thin:@//"),
    SQLITE("sqlite", 0, "jdbc:sqlite:"),
    H2("h2", 9092, "jdbc:h2:");

    companion object {
        fun fromString(type: String): DatabaseType {
            return entries.find { it.typeName.equals(type, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported database type: $type")
        }
    }
}


/**
 * Connection pool configuration for database connections.
 *
 * @param maxActive Maximum number of active connections in the pool
 * @param minIdle Minimum number of idle connections in the pool
 * @param connectionTimeout Connection timeout (in milliseconds)
 * @param idleTimeout Idle timeout (in milliseconds), default is 10 minutes
 * @param maxLifetime Maximum lifetime of a connection (in milliseconds), default is 30 minutes
 * @param logAbandoned Whether to log abandoned connections
 * @author RollW
 */
data class ConnectionPoolConfig(
    val maxActive: Int = 20,
    val minIdle: Int = 2,
    val connectionTimeout: Long = 30000,
    val idleTimeout: Long = 600000L,
    val maxLifetime: Long = 1800000L,
    val logAbandoned: Boolean = false
)
