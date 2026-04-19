package dev.liveeconomy.api.storage

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.data.model.PriceCandle
import dev.liveeconomy.data.model.TradeOrder

/**
 * Persistence interface for market prices, candle history, and limit orders.
 */
interface PriceStore {
    fun getCurrentPrice(item: ItemKey): Double?
    fun saveCurrentPrice(item: ItemKey, price: Double)
    fun saveAllPrices(prices: Map<ItemKey, Double>)

    fun appendCandle(item: ItemKey, candle: PriceCandle)
    fun getCandles(item: ItemKey, page: Int, pageSize: Int = 10): List<PriceCandle>

    fun saveOrder(order: TradeOrder)
    fun removeOrder(orderId: String)
    fun getOpenOrders(item: ItemKey): List<TradeOrder>
    fun getPlayerOrders(playerUuid: java.util.UUID): List<TradeOrder>
    fun getAllOpenOrders(): List<TradeOrder>
}
