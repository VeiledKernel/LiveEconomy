package dev.liveeconomy.storage.sql

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.api.storage.PriceStore
import dev.liveeconomy.data.model.ItemStats
import dev.liveeconomy.data.model.PriceCandle
import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap

/**
 * SQL-backed [PriceStore].
 *
 * **[saveCurrentPrice] contract (FIX #4):**
 * Persists immediately to DB — not cache-only. The in-memory cache is
 * a read optimisation only; every write goes to the database.
 * [saveAllPrices] uses a batch upsert for efficiency when saving many
 * prices at once (called by AutoSaveTask at end of each tick).
 */
class SqlPriceStore(
    private val conn:    Connection,
    private val mapper:  ItemKeyMapper,
    private val dialect: SqlDialect
) : PriceStore {

    private val priceCache = ConcurrentHashMap<String, Double>()

    private val upsertPriceSql = dialect.upsert("current_prices", "item_id", "price")
    private val upsertStatsSql = dialect.upsert("item_stats", "item_id",
        "buy_volume", "sell_volume", "buy_qty", "sell_qty")

    // ── Current prices ────────────────────────────────────────────────────────

    override fun getCurrentPrice(item: ItemKey): Double? =
        priceCache[item.id] ?: run {
            conn.prepareStatement("SELECT price FROM current_prices WHERE item_id=?").use { ps ->
                ps.setString(1, item.id)
                ps.executeQuery().use { rs ->
                    if (rs.next()) rs.getDouble(1).also { priceCache[item.id] = it } else null
                }
            }
        }

    /** FIX #4 — persists immediately to DB, not cache-only. */
    override fun saveCurrentPrice(item: ItemKey, price: Double) {
        priceCache[item.id] = price
        conn.prepareStatement(upsertPriceSql).use { ps ->
            ps.setString(1, item.id); ps.setDouble(2, price); ps.executeUpdate()
        }
    }

    override fun saveAllPrices(prices: Map<ItemKey, Double>) {
        prices.forEach { (item, price) -> priceCache[item.id] = price }
        conn.prepareStatement(upsertPriceSql).use { ps ->
            for ((item, price) in prices) {
                ps.setString(1, item.id); ps.setDouble(2, price); ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    // ── Candle history ────────────────────────────────────────────────────────

    override fun appendCandle(item: ItemKey, candle: PriceCandle) {
        conn.prepareStatement(
            "INSERT INTO price_candles(item_id,open,high,low,close,volume,timestamp) VALUES(?,?,?,?,?,?,?)"
        ).use { ps ->
            ps.setString(1, item.id); ps.setDouble(2, candle.open); ps.setDouble(3, candle.high)
            ps.setDouble(4, candle.low); ps.setDouble(5, candle.close)
            ps.setDouble(6, candle.volume); ps.setLong(7, candle.timestamp)
            ps.executeUpdate()
        }
    }

    override fun getCandles(item: ItemKey, page: Int, pageSize: Int): List<PriceCandle> {
        val offset = (page - 1) * pageSize
        val result = mutableListOf<PriceCandle>()
        conn.prepareStatement(
            "SELECT open,high,low,close,volume,timestamp FROM price_candles " +
            "WHERE item_id=? ORDER BY timestamp DESC LIMIT ? OFFSET ?"
        ).use { ps ->
            ps.setString(1, item.id); ps.setInt(2, pageSize); ps.setInt(3, offset)
            ps.executeQuery().use { rs ->
                while (rs.next()) result.add(PriceCandle(
                    open = rs.getDouble(1), high = rs.getDouble(2), low = rs.getDouble(3),
                    close = rs.getDouble(4), volume = rs.getDouble(5), timestamp = rs.getLong(6)
                ))
            }
        }
        return result
    }

    // ── Item statistics ───────────────────────────────────────────────────────

    override fun getItemStats(item: ItemKey): ItemStats? =
        conn.prepareStatement("SELECT * FROM item_stats WHERE item_id=?").use { ps ->
            ps.setString(1, item.id)
            ps.executeQuery().use { rs ->
                if (rs.next()) ItemStats(
                    item       = mapper.fromString(rs.getString("item_id")),
                    buyVolume  = rs.getDouble("buy_volume"),
                    sellVolume = rs.getDouble("sell_volume"),
                    buyQty     = rs.getInt("buy_qty"),
                    sellQty    = rs.getInt("sell_qty")
                ) else null
            }
        }

    override fun updateItemStats(item: ItemKey, stats: ItemStats) {
        conn.prepareStatement(upsertStatsSql).use { ps ->
            ps.setString(1, item.id); ps.setDouble(2, stats.buyVolume)
            ps.setDouble(3, stats.sellVolume); ps.setInt(4, stats.buyQty)
            ps.setInt(5, stats.sellQty); ps.executeUpdate()
        }
    }
}
