package dev.liveeconomy.storage.sql

import dev.liveeconomy.api.Lifecycle
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
 * Subclasses supply the [Connection] (SQLite or MySQL) and the
 * appropriate [SqlDialect] for provider-specific SQL syntax.
 *
 * **Phase 4 — Single shared connection (known limitation):**
 * All stores share one [Connection] object. This is safe for Phase 4
 * because all trade execution runs on the main thread (single-threaded
 * access), but it is NOT safe for concurrent multi-threaded access.
 *
 * **Phase 5 migration target:**
 * ```kotlin
 * abstract class SqlStorageProvider(
 *     protected val dataSource: javax.sql.DataSource,  // HikariCP
 *     protected val mapper: ItemKeyMapper
 * )
 * ```
 * Each store will then acquire its own connection per operation,
 * providing proper connection isolation and concurrent safety.
 * No store interface changes are required for this migration.
 *
 * Schema is created via [initSchema] on [start]. All tables use
 * `CREATE TABLE IF NOT EXISTS` — idempotent and safe on reload.
 */
abstract class SqlStorageProvider(
    protected val mapper:  ItemKeyMapper,
    protected val dialect: SqlDialect
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
        _wallet      = SqlWalletStore(connection, dialect)
        _portfolio   = SqlPortfolioStore(connection, mapper, dialect)
        _price       = SqlPriceStore(connection, mapper, dialect)
        _transaction = SqlTransactionStore(connection, mapper)
        _order       = SqlOrderStore(connection, mapper, dialect)
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

    // ── Schema ────────────────────────────────────────────────────────────────

    private fun initSchema() {
        connection.createStatement().use { stmt ->
            // Wallets
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS wallets (
                    uuid    TEXT PRIMARY KEY,
                    balance REAL NOT NULL DEFAULT 0.0 CHECK(balance >= 0)
                )""".trimIndent())

            // Holdings
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS holdings (
                    uuid     TEXT    NOT NULL,
                    item_id  TEXT    NOT NULL,
                    quantity INTEGER NOT NULL CHECK(quantity >= 0),
                    PRIMARY KEY (uuid, item_id)
                )""".trimIndent())

            // P&L
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS pnl (
                    uuid   TEXT PRIMARY KEY,
                    amount REAL NOT NULL DEFAULT 0.0
                )""".trimIndent())

            // Player stats
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_stats (
                    uuid         TEXT PRIMARY KEY,
                    total_buys   INTEGER NOT NULL DEFAULT 0 CHECK(total_buys   >= 0),
                    total_sells  INTEGER NOT NULL DEFAULT 0 CHECK(total_sells  >= 0),
                    wins         INTEGER NOT NULL DEFAULT 0 CHECK(wins         >= 0),
                    total_volume REAL    NOT NULL DEFAULT 0.0,
                    total_roi    REAL    NOT NULL DEFAULT 0.0
                )""".trimIndent())

            // Short positions
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS short_positions (
                    uuid        TEXT    NOT NULL,
                    item_id     TEXT    NOT NULL,
                    quantity    INTEGER NOT NULL CHECK(quantity > 0),
                    entry_price REAL    NOT NULL CHECK(entry_price > 0),
                    collateral  REAL    NOT NULL CHECK(collateral >= 0),
                    PRIMARY KEY (uuid, item_id)
                )""".trimIndent())

            // Prestige
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS prestige (
                    uuid  TEXT PRIMARY KEY,
                    level INTEGER NOT NULL DEFAULT 0 CHECK(level >= 0)
                )""".trimIndent())

            // Current prices (durable)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS current_prices (
                    item_id TEXT PRIMARY KEY,
                    price   REAL NOT NULL CHECK(price >= 0)
                )""".trimIndent())

            // Price candles
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS price_candles (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    item_id   TEXT    NOT NULL,
                    open      REAL    NOT NULL,
                    high      REAL    NOT NULL,
                    low       REAL    NOT NULL,
                    close     REAL    NOT NULL,
                    volume    REAL    NOT NULL DEFAULT 0.0,
                    timestamp INTEGER NOT NULL
                )""".trimIndent())

            // Item stats
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS item_stats (
                    item_id     TEXT PRIMARY KEY,
                    buy_volume  REAL    NOT NULL DEFAULT 0.0,
                    sell_volume REAL    NOT NULL DEFAULT 0.0,
                    buy_qty     INTEGER NOT NULL DEFAULT 0 CHECK(buy_qty  >= 0),
                    sell_qty    INTEGER NOT NULL DEFAULT 0 CHECK(sell_qty >= 0)
                )""".trimIndent())

            // Transactions — action constrained to known values
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT    NOT NULL,
                    timestamp   INTEGER NOT NULL,
                    item_id     TEXT    NOT NULL,
                    action      TEXT    NOT NULL CHECK(action IN ('BUY','SELL','SHORT_OPEN','SHORT_CLOSE')),
                    quantity    INTEGER NOT NULL,
                    unit_price  REAL    NOT NULL,
                    total       REAL    NOT NULL
                )""".trimIndent())

            // Orders
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS orders (
                    order_id     TEXT PRIMARY KEY,
                    player_uuid  TEXT    NOT NULL,
                    player_name  TEXT    NOT NULL,
                    item_id      TEXT    NOT NULL,
                    quantity     INTEGER NOT NULL CHECK(quantity > 0),
                    target_price REAL    NOT NULL CHECK(target_price > 0),
                    is_buy_order INTEGER NOT NULL CHECK(is_buy_order IN (0,1)),
                    placed_at    INTEGER NOT NULL,
                    expiry_hours INTEGER NOT NULL DEFAULT 24 CHECK(expiry_hours > 0)
                )""".trimIndent())

            // Indexes
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_candles_item   ON price_candles(item_id, timestamp)")
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_txs_player     ON transactions(player_uuid, timestamp DESC)")
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_orders_item    ON orders(item_id)")
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_orders_player  ON orders(player_uuid)")
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_holdings_uuid  ON holdings(uuid)")
        }
    }
}
