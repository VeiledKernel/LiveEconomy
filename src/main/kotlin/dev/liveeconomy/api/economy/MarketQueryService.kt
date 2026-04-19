package dev.liveeconomy.api.economy

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.data.model.ItemStats
import dev.liveeconomy.data.model.MarketItem
import dev.liveeconomy.data.model.PriceCandle
import dev.liveeconomy.data.model.TradeOrder
import java.util.UUID

/**
 * Market data and history queries.
 *
 * Safe to call from any thread — read-only, no state changes.
 *
 * Inject this interface when you only need market data:
 * ```kotlin
 * class MarketHistoryGUI(private val query: MarketQueryService)
 * ```
 *
 * Access via:
 * ```kotlin
 * val query = LiveEconomyAPI.get().query()
 * ```
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
interface MarketQueryService {

    /**
     * Retrieve the full [MarketItem] record for [item], including current
     * price, volatility, category, and tick history.
     *
     * Returns null if [item] is not listed.
     */
    fun getItem(item: ItemKey): MarketItem?

    /**
     * All registered market items, keyed by their [ItemKey].
     */
    fun getAllItems(): Map<ItemKey, MarketItem>

    /**
     * Price history for [item] as a list of [PriceCandle] records,
     * ordered from oldest to newest.
     *
     * @param item  the item to query
     * @param page  1-based page number (each page holds 10 candles)
     * @return candles for that page, or empty list if none
     */
    fun getPriceHistory(item: ItemKey, page: Int = 1): List<PriceCandle>

    /**
     * All open limit orders for [item], sorted by target price descending
     * (highest buy bids first, then sell asks).
     */
    fun getOpenOrders(item: ItemKey): List<TradeOrder>

    /**
     * All open limit orders placed by [playerUuid].
     */
    fun getPlayerOrders(playerUuid: UUID): List<TradeOrder>

    /**
     * Per-item trading statistics for [item].
     * Returns null if [item] has no recorded trades.
     */
    fun getItemStats(item: ItemKey): ItemStats?

    /**
     * Items sorted by total trade volume, highest first.
     *
     * @param limit maximum number of items to return
     */
    fun getTopItemsByVolume(limit: Int = 10): List<Pair<ItemKey, Double>>
}
