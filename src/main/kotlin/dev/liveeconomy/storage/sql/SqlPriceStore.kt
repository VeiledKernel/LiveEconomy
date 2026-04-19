package dev.liveeconomy.storage.sql

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.api.storage.PriceStore
import dev.liveeconomy.data.model.ItemStats
import dev.liveeconomy.data.model.PriceCandle
import java.sql.Connection

/**
 * SQL-backed [PriceStore]. Current prices cached in memory; candles append-only.
 */
class SqlPriceStore(
    private val conn:   Connection,
    private val mapper: ItemKeyMapper
) : PriceStore {

    private val priceCache = java.util.concurrent.ConcurrentHashMap<String, Double>()

    override fun getCurrentPrice(item: ItemKey): Double? =
        priceCache[item.id] ?: conn.prepareStatement(
            "SELECT price FROM current_prices WHERE item_id=?"
        ).use { ps ->
            ps.setString(1, item.id)
            ps.executeQuery().use { rs -> if (rs.next()) rs.getDouble(1).also { priceCache[item.id] = it } else null }
        }

    override fun saveCurrentPrice(item: ItemKey, price: Double) {
        priceCache[item.id] = price
    }

    override fun saveAllPrices(prices: Map<ItemKey, Double>) {
        priceCache.putAll(prices.mapKeys { it.key.id })
        conn.prepareStatement(
            "INSERT INTO current_prices(item_id,price) VALUES(?,?) ON CONFLICT(item_id) DO UPDATE SET price=excluded.price"
        ).use { ps ->
            for ((item, price) in prices) {
                ps.setString(1, item.id); ps.setDouble(2, price); ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    override fun appendCandle(item: ItemKey, candle: PriceCandle) {
        conn.prepareStatement(
            "INSERT INTO price_candles(item_id,open,high,low,close,volume,timestamp) VALUES(?,?,?,?,?,?,?)"
        ).use { ps ->
            ps.setString(1, item.id); ps.setDouble(2, candle.open); ps.setDouble(3, candle.high)
            ps.setDouble(4, candle.low); ps.setDouble(5, candle.close); ps.setDouble(6, candle.volume)
            ps.setLong(7, candle.timestamp); ps.executeUpdate()
        }
    }

    override fun getCandles(item: ItemKey, page: Int, pageSize: Int): List<PriceCandle> {
        val offset = (page - 1) * pageSize
        val result = mutableListOf<PriceCandle>()
        conn.prepareStatement(
            "SELECT open,high,low,close,volume,timestamp FROM price_candles WHERE item_id=? ORDER BY timestamp DESC LIMIT ? OFFSET ?"
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
        conn.prepareStatement(
            "INSERT INTO item_stats(item_id,buy_volume,sell_volume,buy_qty,sell_qty) VALUES(?,?,?,?,?) ON CONFLICT(item_id) DO UPDATE SET buy_volume=excluded.buy_volume,sell_volume=excluded.sell_volume,buy_qty=excluded.buy_qty,sell_qty=excluded.sell_qty"
        ).use { ps ->
            ps.setString(1, item.id); ps.setDouble(2, stats.buyVolume); ps.setDouble(3, stats.sellVolume)
            ps.setInt(4, stats.buyQty); ps.setInt(5, stats.sellQty); ps.executeUpdate()
        }
    }
}
