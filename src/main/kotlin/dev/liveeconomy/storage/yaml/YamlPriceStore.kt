package dev.liveeconomy.storage.yaml

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.storage.PriceStore
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.data.model.ItemStats
import dev.liveeconomy.data.model.PriceCandle
import dev.liveeconomy.storage.yaml.AtomicYamlWriter
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * YAML-backed [PriceStore].
 *
 * Two files:
 *  - `current-prices.yml` — live prices, saved every tick + on shutdown
 *  - `price-history.yml`  — OHLCV candle history, append-only per item
 */
class YamlPriceStore(
    private val pricesFile:  File,
    private val historyFile: File,
    private val statsFile:   File,
    private val mapper:      ItemKeyMapper
) : PriceStore {

    private val currentPrices = ConcurrentHashMap<String, Double>()       // itemId → price
    private val candles       = ConcurrentHashMap<String, MutableList<PriceCandle>>() // itemId → candles
    private val itemStats     = ConcurrentHashMap<String, ItemStats>()
    private val priceLock     = Any()
    private val historyLock   = Any()

    fun load() {
        loadPrices()
        loadHistory()
        loadStats()
    }

    private fun loadPrices() {
        if (!pricesFile.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(pricesFile)
        yaml.getConfigurationSection("prices")?.getKeys(false)?.forEach { itemId ->
            currentPrices[itemId] = yaml.getDouble("prices.$itemId")
        }
    }

    private fun loadHistory() {
        if (!historyFile.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(historyFile)
        yaml.getConfigurationSection("history")?.getKeys(false)?.forEach { itemId ->
            val list = mutableListOf<PriceCandle>()
            val count = yaml.getInt("history.$itemId.count", 0)
            for (i in 0 until count) {
                val base = "history.$itemId.candles.$i"
                list += PriceCandle(
                    open      = yaml.getDouble("$base.open"),
                    high      = yaml.getDouble("$base.high"),
                    low       = yaml.getDouble("$base.low"),
                    close     = yaml.getDouble("$base.close"),
                    volume    = yaml.getDouble("$base.volume"),
                    timestamp = yaml.getLong("$base.timestamp")
                )
            }
            candles[itemId] = list
        }
    }

    private fun loadStats() {
        if (!statsFile.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(statsFile)
        yaml.getConfigurationSection("stats")?.getKeys(false)?.forEach { itemId ->
            val item = try { mapper.fromString(itemId) } catch (e: Exception) { return@forEach }
            itemStats[itemId] = ItemStats(
                item       = item,
                buyVolume  = yaml.getDouble("stats.$itemId.buyVolume"),
                sellVolume = yaml.getDouble("stats.$itemId.sellVolume"),
                buyQty     = yaml.getInt("stats.$itemId.buyQty"),
                sellQty    = yaml.getInt("stats.$itemId.sellQty")
            )
        }
    }

    // ── Current prices ────────────────────────────────────────────────────────

    override fun getCurrentPrice(item: ItemKey): Double? = currentPrices[item.id]

    override fun saveCurrentPrice(item: ItemKey, price: Double) {
        currentPrices[item.id] = price
    }

    override fun saveAllPrices(prices: Map<ItemKey, Double>) {
        prices.forEach { (item, price) -> currentPrices[item.id] = price }
        synchronized(priceLock) {
            val yaml = YamlConfiguration()
            currentPrices.forEach { (id, price) -> yaml.set("prices.$id", price) }
            pricesFile.parentFile?.mkdirs()
            AtomicYamlWriter.save(yaml, pricesFile)
        }
    }

    // ── Candle history ────────────────────────────────────────────────────────

    override fun appendCandle(item: ItemKey, candle: PriceCandle) {
        val list = candles.getOrPut(item.id) { mutableListOf() }
        synchronized(historyLock) {
            list.add(candle)
            // Cap at 1000 candles per item — oldest pruned first
            if (list.size > 1000) list.removeAt(0)
            persistCandles(item.id, list)
        }
    }

    override fun getCandles(item: ItemKey, page: Int, pageSize: Int): List<PriceCandle> {
        val list   = candles[item.id] ?: return emptyList()
        val start  = ((page - 1) * pageSize).coerceAtLeast(0)
        val end    = (start + pageSize).coerceAtMost(list.size)
        return if (start >= list.size) emptyList() else list.subList(start, end)
    }

    private fun persistCandles(itemId: String, list: List<PriceCandle>) {
        // Append-only: rewrite only the affected item's candle section
        val yaml = if (historyFile.exists()) YamlConfiguration.loadConfiguration(historyFile)
                   else YamlConfiguration()
        yaml.set("history.$itemId.count", list.size)
        list.forEachIndexed { i, c ->
            val base = "history.$itemId.candles.$i"
            yaml.set("$base.open",      c.open)
            yaml.set("$base.high",      c.high)
            yaml.set("$base.low",       c.low)
            yaml.set("$base.close",     c.close)
            yaml.set("$base.volume",    c.volume)
            yaml.set("$base.timestamp", c.timestamp)
        }
        historyFile.parentFile?.mkdirs()
        AtomicYamlWriter.save(yaml, historyFile)
    }

    // ── Item statistics ───────────────────────────────────────────────────────

    override fun getItemStats(item: ItemKey): ItemStats? = itemStats[item.id]

    override fun updateItemStats(item: ItemKey, stats: ItemStats) {
        itemStats[item.id] = stats
        val yaml = if (statsFile.exists()) YamlConfiguration.loadConfiguration(statsFile)
                   else YamlConfiguration()
        val base = "stats.${item.id}"
        yaml.set("$base.buyVolume",  stats.buyVolume)
        yaml.set("$base.sellVolume", stats.sellVolume)
        yaml.set("$base.buyQty",     stats.buyQty)
        yaml.set("$base.sellQty",    stats.sellQty)
        statsFile.parentFile?.mkdirs()
        AtomicYamlWriter.save(yaml, statsFile)
    }
}
