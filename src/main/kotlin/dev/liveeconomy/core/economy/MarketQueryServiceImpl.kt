package dev.liveeconomy.core.economy

import dev.liveeconomy.api.economy.MarketQueryService
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.storage.PriceStore
import dev.liveeconomy.data.model.ItemStats
import dev.liveeconomy.data.model.MarketItem
import dev.liveeconomy.data.model.PriceCandle
import dev.liveeconomy.data.model.TradeOrder
import java.util.UUID

/**
 * [MarketQueryService] implementation — read-only market data queries.
 *
 * All methods are safe to call from any thread — read-only, no state changes.
 *
 * [PriceServiceImpl] is the item registry source of truth.
 * [PriceStore] provides candle history and item statistics.
 * [OrderBook] provides open order data.
 */
class MarketQueryServiceImpl(
    private val prices:    PriceServiceImpl,
    private val store:     PriceStore,
    private val orderBook: OrderBook
) : MarketQueryService {

    override fun getItem(item: ItemKey): MarketItem? =
        prices.getItem(item)

    override fun getAllItems(): Map<ItemKey, MarketItem> =
        prices.getAllItems().values.associateBy { it.itemKey }

    override fun getPriceHistory(item: ItemKey, page: Int): List<PriceCandle> =
        store.getCandles(item, page, pageSize = 10)

    override fun getOpenOrders(item: ItemKey): List<TradeOrder> =
        orderBook.getOpenOrders(item)

    override fun getPlayerOrders(playerUuid: UUID): List<TradeOrder> =
        orderBook.getPlayerOrders(playerUuid)

    override fun getItemStats(item: ItemKey): ItemStats? =
        store.getItemStats(item)

    override fun getTopItemsByVolume(limit: Int): List<Pair<ItemKey, Double>> =
        prices.getAllItems().values
            .mapNotNull { item ->
                store.getItemStats(item.itemKey)?.let { stats ->
                    item.itemKey to stats.totalVolume
                }
            }
            .sortedByDescending { it.second }
            .take(limit)
}
