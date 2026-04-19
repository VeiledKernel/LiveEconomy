package dev.liveeconomy.storage.yaml

import dev.liveeconomy.api.storage.OrderStore
import dev.liveeconomy.api.storage.PortfolioStore
import dev.liveeconomy.api.storage.PriceStore
import dev.liveeconomy.api.storage.StorageProvider
import dev.liveeconomy.api.storage.TransactionStore
import dev.liveeconomy.api.storage.WalletStore
import dev.liveeconomy.core.item.BukkitItemKeyMapper
import java.io.File

/**
 * YAML implementation of [StorageProvider].
 *
 * All five stores backed by YAML files in [dataFolder].
 * Loaded on [start], closed (no-op for YAML) on [stop].
 *
 * **Transaction contract:** YAML has no real rollback.
 * Multi-step writes are best-effort sequential — partial writes are
 * possible on crash. Use SQL backend for production deployments.
 * See [dev.liveeconomy.api.storage.TransactionScope] contract.
 *
 * File layout:
 * ```
 * plugins/LiveEconomy/
 *   orders.yml
 *   wallets.yml
 *   portfolios.yml
 *   current-prices.yml
 *   price-history.yml
 *   item-stats.yml
 *   transactions.yml
 * ```
 */
class YamlStorageProvider(
    private val dataFolder: File,
    private val mapper:     BukkitItemKeyMapper
) : StorageProvider {

    private val _orders      = YamlOrderStore(
        file   = File(dataFolder, "orders.yml"),
        mapper = mapper
    )
    private val _wallets     = YamlWalletStore(
        file = File(dataFolder, "wallets.yml")
    )
    private val _portfolios  = YamlPortfolioStore(
        file   = File(dataFolder, "portfolios.yml"),
        mapper = mapper
    )
    private val _prices      = YamlPriceStore(
        pricesFile  = File(dataFolder, "current-prices.yml"),
        historyFile = File(dataFolder, "price-history.yml"),
        statsFile   = File(dataFolder, "item-stats.yml"),
        mapper      = mapper
    )
    private val _transactions = YamlTransactionStore(
        file   = File(dataFolder, "transactions.yml"),
        mapper = mapper
    )

    @Volatile private var started = false

    override fun start() {
        if (started) return
        dataFolder.mkdirs()
        _wallets.load()
        _portfolios.load()
        _prices.load()
        _transactions.load()
        // Orders loaded separately via OrderBook.init() after MarketRegistry is ready
        started = true
    }

    override fun stop() {
        // YAML stores flush on every write — no explicit flush needed on shutdown.
        // This no-op satisfies the Lifecycle contract.
        started = false
    }

    override fun wallet():      WalletStore      = _wallets
    override fun portfolio():   PortfolioStore   = _portfolios
    override fun price():       PriceStore       = _prices
    override fun transaction(): TransactionStore  = _transactions
    override fun order():       OrderStore        = _orders
}
