package dev.liveeconomy.core.economy.port

import dev.liveeconomy.api.economy.result.OrderResult
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.data.model.TradeOrder
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Internal port for limit order book operations.
 *
 * Abstracts [dev.liveeconomy.core.economy.OrderBook] from its callers.
 *
 * // Internal interface — not part of public api/
 */
internal interface OrderBookPort {
    fun place(player: Player, item: ItemKey, quantity: Int, targetPrice: Double, isBuyOrder: Boolean): OrderResult
    fun cancel(player: Player, orderId: String): OrderResult
    fun getTriggeredOrders(item: ItemKey, currentPrice: Double): List<TradeOrder>
    fun markFilled(orderId: String)
    fun pruneExpired(item: ItemKey): Int
    fun getPlayerOrders(uuid: UUID): List<TradeOrder>
}
