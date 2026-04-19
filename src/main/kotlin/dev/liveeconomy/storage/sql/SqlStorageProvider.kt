package dev.liveeconomy.storage.sql

import com.zaxxer.hikari.HikariDataSource
import dev.liveeconomy.api.Lifecycle
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.api.storage.OrderStore
import dev.liveeconomy.api.storage.PortfolioStore
import dev.liveeconomy.api.storage.PriceStore
import dev.liveeconomy.api.storage.StorageProvider
import dev.liveeconomy.api.storage.TransactionStore
import dev.liveeconomy.api.storage.WalletStore
import javax.sql.DataSource

/**
 * Abstract base for SQL-backed [StorageProvider] implementations.
 *
 * Uses a [HikariDataSource] for connection pooling — each store acquires
 * its own connection per operation from the pool, providing proper
 * isolation for concurrent access.
 *
 * Subclasses supply the configured [HikariDataSource] for their backend
 * (SQLite or MySQL). Schema is initialised on [start] via [initSchema].
 *
 * **DEBT-1: CLEARED** — migrated from single shared Connection to
 * HikariCP DataSource. Each store receives the DataSource and acquires
 * a connection per operation, then releases it back to the pool.
 *
 * **DEBT-2: CLEARED** — schema migrations handled via [SchemaVersion]
 * table. See [runMigrations].
 */
abstract class SqlStorageProvider(
    protected val mapper:  ItemKeyMapper,
    protected val dialect: SqlDialect
) : StorageProvider {

    protected abstract val dataSource: HikariDataSource

    private lateinit var _wallet:      SqlWalletStore
    private lateinit var _portfolio:   SqlPortfolioStore
    private lateinit var _price:       SqlPriceStore
    private lateinit var _transaction: SqlTransactionStore
    private lateinit var _order:       SqlOrderStore

    @Volatile private var started = false

    override fun start() {
        if (started) return
        initSchema()
        runMigrations()
        _wallet      = SqlWalletStore(dataSource, dialect)
        _portfolio   = SqlPortfolioStore(dataSource, mapper, dialect)
        _price       = SqlPriceStore(dataSource, mapper, dialect)
        _transaction = SqlTransactionStore(dataSource, mapper)
        _order       = SqlOrderStore(dataSource, mapper, dialect)
        started = true
    }

    override fun stop() {
        if (!started) return
        if (!dataSource.isClosed) dataSource.close()
        started = false
    }

    override fun wallet():      WalletStore      = _wallet
    override fun portfolio():   PortfolioStore   = _portfolio
    override fun price():       PriceStore       = _price
    override fun transaction(): TransactionStore  = _transaction
    override fun order():       OrderStore        = _order

    // ── Schema ────────────────────────────────────────────────────────────────

    private fun initSchema() {
        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                // Migration tracking table — must exist before runMigrations()
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS schema_version (
                        version     INTEGER PRIMARY KEY,
                        applied_at  INTEGER NOT NULL,
                        description TEXT    NOT NULL
                    )""".trimIndent())

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS wallets (
                        uuid    TEXT PRIMARY KEY,
                        balance REAL NOT NULL DEFAULT 0.0 CHECK(balance >= 0)
                    )""".trimIndent())

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS holdings (
                        uuid     TEXT    NOT NULL,
                        item_id  TEXT    NOT NULL,
                        quantity INTEGER NOT NULL CHECK(quantity >= 0),
                        PRIMARY KEY (uuid, item_id)
                    )""".trimIndent())

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS pnl (
                        uuid   TEXT PRIMARY KEY,
                        amount REAL NOT NULL DEFAULT 0.0
                    )""".trimIndent())

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_stats (
                        uuid         TEXT PRIMARY KEY,
                        total_buys   INTEGER NOT NULL DEFAULT 0 CHECK(total_buys   >= 0),
                        total_sells  INTEGER NOT NULL DEFAULT 0 CHECK(total_sells  >= 0),
                        wins         INTEGER NOT NULL DEFAULT 0 CHECK(wins         >= 0),
                        total_volume REAL    NOT NULL DEFAULT 0.0,
                        total_roi    REAL    NOT NULL DEFAULT 0.0
                    )""".trimIndent())

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS short_positions (
                        uuid        TEXT    NOT NULL,
                        item_id     TEXT    NOT NULL,
                        quantity    INTEGER NOT NULL CHECK(quantity > 0),
                        entry_price REAL    NOT NULL CHECK(entry_price > 0),
                        collateral  REAL    NOT NULL CHECK(collateral >= 0),
                        PRIMARY KEY (uuid, item_id)
                    )""".trimIndent())

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS prestige (
                        uuid  TEXT PRIMARY KEY,
                        level INTEGER NOT NULL DEFAULT 0 CHECK(level >= 0)
                    )""".trimIndent())

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS current_prices (
                        item_id TEXT PRIMARY KEY,
                        price   REAL NOT NULL CHECK(price >= 0)
                    )""".trimIndent())

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

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS item_stats (
                        item_id     TEXT PRIMARY KEY,
                        buy_volume  REAL    NOT NULL DEFAULT 0.0,
                        sell_volume REAL    NOT NULL DEFAULT 0.0,
                        buy_qty     INTEGER NOT NULL DEFAULT 0 CHECK(buy_qty  >= 0),
                        sell_qty    INTEGER NOT NULL DEFAULT 0 CHECK(sell_qty >= 0)
                    )""".trimIndent())

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

    // ── Schema migrations ─────────────────────────────────────────────────────

    /**
     * Lightweight migration runner. Each migration is identified by a version
     * integer and recorded in [schema_version] so it runs exactly once.
     *
     * To add a new migration:
     * 1. Add a new entry to [MIGRATIONS] with the next version number.
     * 2. Write the SQL. It will run automatically on next server start.
     * 3. Never modify an existing migration — add a new one instead.
     */
    private fun runMigrations() {
        dataSource.connection.use { conn ->
            val applied = mutableSetOf<Int>()
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT version FROM schema_version").use { rs ->
                    while (rs.next()) applied.add(rs.getInt(1))
                }
            }

            for ((version, description, sql) in MIGRATIONS) {
                if (version in applied) continue
                conn.autoCommit = false
                try {
                    conn.createStatement().use { it.executeUpdate(sql) }
                    conn.prepareStatement(
                        "INSERT INTO schema_version(version, applied_at, description) VALUES(?,?,?)"
                    ).use { ps ->
                        ps.setInt(1, version)
                        ps.setLong(2, System.currentTimeMillis())
                        ps.setString(3, description)
                        ps.executeUpdate()
                    }
                    conn.commit()
                    System.out.println("[LiveEconomy] Applied DB migration v$version: $description")
                } catch (e: Exception) {
                    conn.rollback()
                    throw RuntimeException("DB migration v$version failed: ${e.message}", e)
                } finally {
                    conn.autoCommit = true
                }
            }
        }
    }

    companion object {
        /**
         * Ordered list of schema migrations.
         * Triple: (version, description, SQL).
         * Never edit existing entries — add new ones at the end.
         */
        private val MIGRATIONS: List<Triple<Int, String, String>> = listOf(
            // v4.0 baseline — tables created by initSchema, this records the starting point
            Triple(1, "v4.0 baseline schema", "SELECT 1")
            // Future migrations go here:
            // Triple(2, "add player_notes column", "ALTER TABLE player_stats ADD COLUMN notes TEXT")
        )
    }
}
