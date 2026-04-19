package dev.liveeconomy.storage.sql.sqlite

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.data.config.StorageConfig
import dev.liveeconomy.storage.sql.SqlStorageProvider
import dev.liveeconomy.storage.sql.SqliteDialect
import java.io.File

/**
 * SQLite implementation of [SqlStorageProvider].
 *
 * Uses HikariCP with `maximumPoolSize=1` — SQLite supports one writer
 * at a time, so a single-connection pool is the correct configuration.
 * WAL journal mode allows concurrent reads while a write is in progress.
 *
 * DEBT-1: CLEARED — pool-backed via HikariCP.
 */
class SqliteStorageProvider(
    private val dataFolder: File,
    private val config:     StorageConfig,
    mapper:                 ItemKeyMapper
) : SqlStorageProvider(mapper, SqliteDialect) {

    override val dataSource: HikariDataSource by lazy {
        val dbFile = File(dataFolder, config.sqliteFile).also { it.parentFile?.mkdirs() }
        HikariDataSource(HikariConfig().apply {
            jdbcUrl          = "jdbc:sqlite:${dbFile.absolutePath}"
            driverClassName  = "org.sqlite.JDBC"
            maximumPoolSize  = 1      // SQLite is single-writer
            connectionInitSql = "PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON"
            poolName         = "LiveEconomy-SQLite"
        })
    }
}
