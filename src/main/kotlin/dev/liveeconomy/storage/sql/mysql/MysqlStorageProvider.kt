package dev.liveeconomy.storage.sql.mysql

import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.data.config.StorageConfig
import dev.liveeconomy.storage.sql.MysqlDialect
import dev.liveeconomy.storage.sql.SqlDialect
import dev.liveeconomy.storage.sql.SqlStorageProvider
import java.sql.Connection
import java.sql.DriverManager

/**
 * MySQL/MariaDB implementation of [SqlStorageProvider].
 *
 * Uses [MysqlDialect] for `ON DUPLICATE KEY UPDATE` upsert syntax —
 * MySQL does not support SQLite's `ON CONFLICT` syntax.
 *
 * Driver: `com.mysql.cj.jdbc.Driver` loaded via reflection.
 *
 * **Phase 5 migration:** Replace single [connection] with HikariCP
 * DataSource for connection pooling. See [SqlStorageProvider] KDoc.
 */
class MysqlStorageProvider(
    private val config: StorageConfig,
    mapper:             ItemKeyMapper
) : SqlStorageProvider(mapper, MysqlDialect) {

    override val connection: Connection by lazy {
        Class.forName("com.mysql.cj.jdbc.Driver")
        val url = "jdbc:mysql://${config.mysqlHost}:${config.mysqlPort}/${config.mysqlDatabase}" +
                  "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        DriverManager.getConnection(url, config.mysqlUsername, config.mysqlPassword)
    }
}
