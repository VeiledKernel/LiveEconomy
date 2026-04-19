package dev.liveeconomy.core.economy.port

import dev.liveeconomy.api.economy.result.OrderResult
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.data.model.TradeOrder
import java.util.UUID

/**
 * Internal port for limit order book operations.
 *
 * Uses UUID + playerName instead of [org.bukkit.entity.Player] — keeping
 * core/ free of Bukkit entity types. [TradeServiceImpl] extracts
 * player.uniqueId and player.name at the API boundary before calling this.
 *
 * // Internal interface — not part of public api/
 */
internal interface OrderBookPort {
    fun place(
        playerUuid:  UUID,
        playerName:  String,
        item:        ItemKey,
        quantity:    Int,
        targetPrice: Double,
        isBuyOrder:  Boolean
    ): OrderResult
    fun cancel(playerUuid: UUID, orderId: String): OrderResult
    fun getOpenOrders(item: ItemKey): List<TradeOrder>
    fun getTriggeredOrders(item: ItemKey, currentPrice: Double): List<TradeOrder>
    fun markFilled(orderId: String)
    fun pruneExpired(item: ItemKey): Int
    fun getPlayerOrders(uuid: UUID): List<TradeOrder>
}
