package dev.liveeconomy.core.economy

import dev.liveeconomy.api.economy.PriceService
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.api.storage.PriceStore
import dev.liveeconomy.data.model.MarketItem
import java.util.concurrent.ConcurrentHashMap

/**
 * [PriceService] implementation — read-only price queries.
 *
 * The item registry is the in-memory source of truth for prices.
 * [PriceStore] is used only for persistence (candle history, startup restore).
 *
 * Thread safety: item registry uses [ConcurrentHashMap]. Price fields on
 * [MarketItem] are @Volatile — safe to read from any thread.
 */
class PriceServiceImpl(
    private val store:      PriceStore,
    private val priceModel: PriceModelImpl,
    private val mapper:     ItemKeyMapper
) : PriceService {

    // ── Item registry — populated by CategoryLoader at startup ────────────────
    private val items = ConcurrentHashMap<String, MarketItem>() // keyed by ItemKey.id

    /** Called by [CategoryLoader] after loading categories/*.yml. */
    fun register(item: MarketItem) {
        items[item.itemKey.id] = item
        // Restore persisted price if available
        store.getCurrentPrice(item.itemKey)?.let { saved ->
            item.currentPrice  = saved
            item.previousPrice = saved
            item.bidPrice      = priceModel.bid(item)
            item.askPrice      = priceModel.ask(item)
        }
    }

    fun clearAll() = items.clear()

    // ── PriceService interface ────────────────────────────────────────────────

    override fun getPrice(item: ItemKey): Double?  = items[item.id]?.currentPrice
    override fun getBid(item: ItemKey): Double?    = items[item.id]?.bidPrice
    override fun getAsk(item: ItemKey): Double?    = items[item.id]?.askPrice
    override fun isListed(item: ItemKey): Boolean  = items.containsKey(item.id)
    override fun getListedItems(): Set<ItemKey>    = items.values.map { it.itemKey }.toSet()

    override fun getPriceChangePercent(item: ItemKey): Double? =
        items[item.id]?.priceChangePercent

    override fun getIndex(): Double = priceModel.computeIndex(items.values)

    // ── Internal access (not in public API) ───────────────────────────────────

    /** Direct [MarketItem] access for internal engine use. */
    fun getItem(item: ItemKey): MarketItem?              = items[item.id]
    fun getAllItems(): Map<String, MarketItem>            = items
    fun getItemsByCategory(categoryId: String): List<MarketItem> =
        items.values.filter { it.category.id == categoryId }

    /** Persist all current prices — called by AutoSaveTask. */
    fun persistAllPrices() =
        store.saveAllPrices(items.mapKeys { it.key.let { id ->
            items[id]!!.itemKey
        }}.mapValues { it.value.currentPrice })

    /** Reset tick tracking on all items — called at start of each tick. */
    fun openAllTicks() = items.values.forEach { it.openTick() }

    /** Reset trade counters — called at end of each tick. */
    fun closeAllTicks() = items.values.forEach { it.resetTradeCount() }
}
