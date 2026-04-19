package dev.liveeconomy.core.economy

import dev.liveeconomy.api.economy.MarketQueryService
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.storage.PriceStore
import dev.liveeconomy.core.economy.port.OrderBookPort
import dev.liveeconomy.core.market.MarketRegistry
import dev.liveeconomy.data.model.ItemStats
import dev.liveeconomy.data.model.MarketItem
import dev.liveeconomy.data.model.PriceCandle
import dev.liveeconomy.data.model.TradeOrder
import java.util.UUID

/**
 * [MarketQueryService] implementation — read-only market data.
 *
 * Uses [MarketRegistry] for item lookups (not [PriceServiceImpl]) and
 * [PriceStore] for candle history. Clean read-only class.
 */
class MarketQueryServiceImpl(
    private val registry:  MarketRegistry,
    private val store:     PriceStore,
    private val orderBook: OrderBookPort
) : MarketQueryService {

    override fun getItem(item: ItemKey): MarketItem?          = registry.getItem(item)
    override fun getAllItems(): Map<ItemKey, MarketItem>       = registry.getAllItems().values.associateBy { it.itemKey }
    override fun getPriceHistory(item: ItemKey, page: Int): List<PriceCandle> = store.getCandles(item, page)
    override fun getOpenOrders(item: ItemKey): List<TradeOrder> = orderBook.getOpenOrders(item)
    override fun getPlayerOrders(playerUuid: UUID): List<TradeOrder>          = orderBook.getPlayerOrders(playerUuid)
    override fun getItemStats(item: ItemKey): ItemStats?                       = store.getItemStats(item)
    override fun getTopItemsByVolume(limit: Int): List<Pair<ItemKey, Double>>  =
        registry.allItems()
            .mapNotNull { item -> store.getItemStats(item.itemKey)?.let { item.itemKey to it.totalVolume } }
            .sortedByDescending { it.second }
            .take(limit)
}
