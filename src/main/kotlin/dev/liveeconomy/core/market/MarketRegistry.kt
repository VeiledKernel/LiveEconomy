package dev.liveeconomy.core.market

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.storage.PriceStore
import dev.liveeconomy.core.economy.PriceModelImpl
import dev.liveeconomy.data.model.MarketItem
import java.util.concurrent.ConcurrentHashMap

/**
 * Owns the in-memory market item registry.
 *
 * Extracted from [dev.liveeconomy.core.economy.PriceServiceImpl] to give
 * each class a single responsibility:
 *  - [MarketRegistry] — owns registration, item lookup, price restoration
 *  - [PriceServiceImpl] — owns the API-facing read surface
 *  - [MarketTicker] — owns tick lifecycle
 *
 * Populated at startup by [dev.liveeconomy.platform.config.CategoryLoader].
 * Read by PriceServiceImpl and TradeServiceImpl via [PriceRegistry] port.
 *
 * // No interface: internal registry, single implementation.
 */
class MarketRegistry(
    private val store: PriceStore,
    private val model: PriceModelImpl
) {
    private val items = ConcurrentHashMap<String, MarketItem>() // keyed by ItemKey.id

    fun register(item: MarketItem) {
        // Restore persisted price if available
        store.getCurrentPrice(item.itemKey)?.let { saved ->
            item.currentPrice  = saved
            item.previousPrice = saved
            item.bidPrice      = model.bid(item)
            item.askPrice      = model.ask(item)
        }
        items[item.itemKey.id] = item
    }

    fun clearAll() = items.clear()

    fun getItem(key: ItemKey): MarketItem?                   = items[key.id]
    fun getAllItems(): Map<String, MarketItem>                = items
    fun getByCategory(categoryId: String): List<MarketItem>  =
        items.values.filter { it.category.id == categoryId }
    fun isListed(key: ItemKey): Boolean                      = items.containsKey(key.id)
    fun allItems(): Collection<MarketItem>                   = items.values

    /** Persist all current prices — called by AutoSaveTask. */
    fun persistAllPrices() {
        val prices = items.values.associate { it.itemKey to it.currentPrice }
        store.saveAllPrices(prices)
    }
}
