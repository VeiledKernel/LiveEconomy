package dev.liveeconomy.data.config

/**
 * Typed representation of the `storage:` block in config.yml.
 */
data class StorageConfig(
    val type:                    StorageType,
    val sqliteFile:              String,
    val mysqlHost:               String,
    val mysqlPort:               Int,
    val mysqlDatabase:           String,
    val mysqlUsername:           String,
    val mysqlPassword:           String,
    val mysqlPoolSize:           Int,
    val autosaveIntervalMinutes: Long
)

enum class StorageType {
    YAML, SQLITE, MYSQL;

    companion object {
        fun fromString(value: String): StorageType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: YAML
    }
}
