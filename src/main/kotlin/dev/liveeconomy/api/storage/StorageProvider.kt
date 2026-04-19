package dev.liveeconomy.api.storage

import dev.liveeconomy.api.Lifecycle

/**
 * Factory interface for all storage backends.
 *
 * Implements [Lifecycle] — must be started before use and closed on shutdown.
 * Concrete backends: [dev.liveeconomy.storage.yaml.YamlStorageProvider],
 * [dev.liveeconomy.storage.sql.sqlite.SqliteStorageProvider],
 * [dev.liveeconomy.storage.sql.mysql.MysqlStorageProvider].
 *
 * Adding a custom backend:
 * ```kotlin
 * class RedisStorageProvider(redis: RedisClient) : StorageProvider {
 *     override fun wallet(): WalletStore = RedisWalletStore(redis)
 *     ...
 * }
 * ```
 * Then pass to the composition root — no other files change.
 */
interface StorageProvider : Lifecycle {
    fun wallet():      WalletStore
    fun portfolio():   PortfolioStore
    fun price():       PriceStore
    fun transaction(): TransactionStore
    fun order():       OrderStore
}
