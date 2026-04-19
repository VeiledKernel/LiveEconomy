package dev.liveeconomy.storage.sql

import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.api.storage.OrderStore
import dev.liveeconomy.api.storage.PortfolioStore
import dev.liveeconomy.api.storage.PriceStore
import dev.liveeconomy.api.storage.StorageProvider
import dev.liveeconomy.api.storage.TransactionStore
import dev.liveeconomy.api.storage.WalletStore
import java.sql.Connection

/**
 * Abstract base for SQL-backed [StorageProvider] implementations.
 *
 * Subclasses provide the [Connection] (SQLite or MySQL).
 * This class owns the store implementations and lifecycle.
 *
 * **Transaction contract:** real JDBC rollback via [SqlTransactionScope].
 * Multi-step writes are fully atomic — no partial writes on crash.
 *
 * Schema is created via [initSchema] on [start]. Migrations are manual
 * for Phase 4; auto-migration tooling is a future enhancement.
 */
abstract class SqlStorageProvider(
    protected val mapper: ItemKeyMapper
) : StorageProvider {

    protected abstract val connection: Connection

    private lateinit var _wallet:      SqlWalletStore
    private lateinit var _portfolio:   SqlPortfolioStore
    private lateinit var _price:       SqlPriceStore
    private lateinit var _transaction: SqlTransactionStore
    private lateinit var _order:       SqlOrderStore

    @Volatile private var started = false

    override fun start() {
        if (started) return
        initSchema()
        _wallet      = SqlWalletStore(connection)
        _portfolio   = SqlPortfolioStore(connection, mapper)
        _price       = SqlPriceStore(connection, mapper)
        _transaction = SqlTransactionStore(connection, mapper)
        _order       = SqlOrderStore(connection, mapper)
        started = true
    }

    override fun stop() {
        if (!started) return
        try { connection.close() } catch (_: Exception) {}
        started = false
    }

    override fun wallet():      WalletStore      = _wallet
    override fun portfolio():   PortfolioStore   = _portfolio
    override fun price():       PriceStore       = _price
    override fun transaction(): TransactionStore  = _transaction
    override fun order():       OrderStore        = _order

    // ── Schema init ───────────────────────────────────────────────────────────

    private fun initSchema() {
        connection.createStatement().use { stmt ->
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS wallets (
                    uuid TEXT PRIMARY KEY,
                    balance REAL NOT NULL DEFAULT 0.0
                )
            """.trimIndent())

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS holdings (
                    uuid    TEXT NOT NULL,
                    item_id TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    PRIMARY KEY (uuid, item_id)
                )
            """.trimIndent())

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS pnl (
                    uuid TEXT PRIMARY KEY,
                    amount REAL NOT NULL DEFAULT 0.0
                )
            """.trimIndent())

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_stats (
                    uuid         TEXT PRIMARY KEY,
                    total_buys   INTEGER NOT NULL DEFAULT 0,
                    total_sells  INTEGER NOT NULL DEFAULT 0,
                    wins         INTEGER NOT NULL DEFAULT 0,
                    total_volume REAL    NOT NULL DEFAULT 0.0,
                    total_roi    REAL    NOT NULL DEFAULT 0.0
                )
            """.trimIndent())

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS short_positions (
                    uuid        TEXT NOT NULL,
                    item_id     TEXT NOT NULL,
                    quantity    INTEGER NOT NULL,
                    entry_price REAL    NOT NULL,
                    collateral  REAL    NOT NULL,
                    PRIMARY KEY (uuid, item_id)
                )
            """.trimIndent())

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS prestige (
                    uuid  TEXT PRIMARY KEY,
                    level INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS current_prices (
                    item_id TEXT PRIMARY KEY,
                    price   REAL NOT NULL
                )
            """.trimIndent())

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS price_candles (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    item_id   TEXT    NOT NULL,
                    open      REAL    NOT NULL,
                    high      REAL    NOT NULL,
                    low       REAL    NOT NULL,
                    close     REAL    NOT NULL,
                    volume    REAL    NOT NULL,
                    timestamp INTEGER NOT NULL
                )
            """.trimIndent())

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS item_stats (
                    item_id     TEXT PRIMARY KEY,
                    buy_volume  REAL    NOT NULL DEFAULT 0.0,
                    sell_volume REAL    NOT NULL DEFAULT 0.0,
                    buy_qty     INTEGER NOT NULL DEFAULT 0,
                    sell_qty    INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT    NOT NULL,
                    timestamp   INTEGER NOT NULL,
                    item_id     TEXT    NOT NULL,
                    action      TEXT    NOT NULL,
                    quantity    INTEGER NOT NULL,
                    unit_price  REAL    NOT NULL,
                    total       REAL    NOT NULL
                )
            """.trimIndent())

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS orders (
                    order_id     TEXT PRIMARY KEY,
                    player_uuid  TEXT    NOT NULL,
                    player_name  TEXT    NOT NULL,
                    item_id      TEXT    NOT NULL,
                    quantity     INTEGER NOT NULL,
                    target_price REAL    NOT NULL,
                    is_buy_order INTEGER NOT NULL,
                    placed_at    INTEGER NOT NULL,
                    expiry_hours INTEGER NOT NULL DEFAULT 24
                )
            """.trimIndent())

            // Indexes for common query patterns
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_candles_item ON price_candles(item_id, timestamp)")
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_txs_player   ON transactions(player_uuid, timestamp DESC)")
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_orders_item  ON orders(item_id)")
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_orders_player ON orders(player_uuid)")
        }
    }
}
