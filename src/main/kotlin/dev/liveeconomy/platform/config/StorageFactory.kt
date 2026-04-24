package dev.liveeconomy.platform.config

import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.api.storage.StorageProvider
import dev.liveeconomy.data.config.StorageConfig
import dev.liveeconomy.data.config.StorageType
import dev.liveeconomy.storage.sql.mysql.MysqlStorageProvider
import dev.liveeconomy.storage.sql.sqlite.SqliteStorageProvider
import dev.liveeconomy.storage.yaml.YamlStorageProvider
import java.io.File

/**
 * Creates the correct [StorageProvider] based on [StorageConfig.type].
 *
 * Called once in the composition root. All three backends are supported:
 * YAML (default), SQLite, MySQL.
 */
object StorageFactory {

    fun create(config: StorageConfig, dataFolder: File, mapper: ItemKeyMapper): StorageProvider =
        when (config.type) {
            StorageType.MYSQL  -> MysqlStorageProvider(config, mapper)
            StorageType.SQLITE -> SqliteStorageProvider(dataFolder, config, mapper)
            StorageType.YAML   -> YamlStorageProvider(dataFolder, mapper)
        }
}
