package dev.liveeconomy.storage.sql.sqlite

import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.data.config.StorageConfig
import dev.liveeconomy.storage.sql.SqlStorageProvider
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * SQLite implementation of [SqlStorageProvider].
 *
 * File: `plugins/LiveEconomy/<config.sqliteFile>`
 * Driver: org.sqlite.JDBC loaded via reflection (no compile-time dep).
 * WAL journal mode enabled for better read concurrency.
 */
class SqliteStorageProvider(
    private val dataFolder: File,
    private val config:     StorageConfig,
    mapper:                 ItemKeyMapper
) : SqlStorageProvider(mapper) {

    override val connection: Connection by lazy {
        Class.forName("org.sqlite.JDBC")
        val dbFile = File(dataFolder, config.sqliteFile)
        dbFile.parentFile?.mkdirs()
        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").also { conn ->
            conn.createStatement().use { it.execute("PRAGMA journal_mode=WAL") }
            conn.createStatement().use { it.execute("PRAGMA foreign_keys=ON") }
        }
    }
}
