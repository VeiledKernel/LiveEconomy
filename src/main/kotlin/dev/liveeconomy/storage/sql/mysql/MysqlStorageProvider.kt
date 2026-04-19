package dev.liveeconomy.storage.sql.mysql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.data.config.StorageConfig
import dev.liveeconomy.storage.sql.MysqlDialect
import dev.liveeconomy.storage.sql.SqlStorageProvider

/**
 * MySQL/MariaDB implementation of [SqlStorageProvider].
 *
 * Uses HikariCP with a configurable pool size for concurrent connections.
 * Pool size defaults to [StorageConfig.mysqlPoolSize] (recommended: 5–10).
 *
 * DEBT-1: CLEARED — pool-backed via HikariCP, not a single shared connection.
 */
class MysqlStorageProvider(
    private val config: StorageConfig,
    mapper:             ItemKeyMapper
) : SqlStorageProvider(mapper, MysqlDialect) {

    override val dataSource: HikariDataSource by lazy {
        HikariDataSource(HikariConfig().apply {
            jdbcUrl         = "jdbc:mysql://${config.mysqlHost}:${config.mysqlPort}/${config.mysqlDatabase}" +
                              "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
            driverClassName = "com.mysql.cj.jdbc.Driver"
            username        = config.mysqlUsername
            password        = config.mysqlPassword
            maximumPoolSize = config.mysqlPoolSize.coerceIn(2, 20)
            minimumIdle     = 2
            connectionTimeout       = 30_000L
            idleTimeout             = 600_000L
            maxLifetime             = 1_800_000L
            poolName                = "LiveEconomy-MySQL"
        })
    }
}
