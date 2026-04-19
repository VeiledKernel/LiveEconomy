package dev.liveeconomy.storage.sql.mysql

import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.data.config.StorageConfig
import dev.liveeconomy.storage.sql.SqlStorageProvider
import java.sql.Connection
import java.sql.DriverManager

/**
 * MySQL/MariaDB implementation of [SqlStorageProvider].
 *
 * Driver: com.mysql.cj.jdbc.Driver loaded via reflection.
 * Uses a single shared connection in Phase 4 — HikariCP connection pool
 * is a planned Phase 5 enhancement for high-concurrency servers.
 */
class MysqlStorageProvider(
    private val config: StorageConfig,
    mapper:             ItemKeyMapper
) : SqlStorageProvider(mapper) {

    override val connection: Connection by lazy {
        Class.forName("com.mysql.cj.jdbc.Driver")
        val url = "jdbc:mysql://${config.mysqlHost}:${config.mysqlPort}/${config.mysqlDatabase}" +
                  "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        DriverManager.getConnection(url, config.mysqlUsername, config.mysqlPassword)
    }
}
