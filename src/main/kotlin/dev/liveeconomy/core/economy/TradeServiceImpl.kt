package dev.liveeconomy.core.economy

import dev.liveeconomy.api.economy.TradeService
import dev.liveeconomy.api.economy.result.OrderResult
import dev.liveeconomy.api.economy.result.ShortResult
import dev.liveeconomy.api.economy.result.TradeResult
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.core.economy.port.OrderBookPort
import dev.liveeconomy.core.economy.port.PriceRegistry
import dev.liveeconomy.core.usecase.CloseShortUseCase
import dev.liveeconomy.core.usecase.ExecuteTradeUseCase
import dev.liveeconomy.core.usecase.OpenShortUseCase
import org.bukkit.entity.Player

/**
 * [TradeService] implementation — thin boundary over use cases.
 *
 * **Does not contain business logic.** All orchestration lives in:
 *  - [ExecuteTradeUseCase] — buy/sell flows
 *  - [OpenShortUseCase]   — short position opening
 *  - [CloseShortUseCase]  — short position closing
 *
 * This class is responsible for:
 *  1. Implementing the public [TradeService] API contract
 *  2. Delegating to the appropriate use case
 *  3. Exposing internal engine methods (processTriggeredOrders) that are
 *     NOT part of the public API
 *
 * **Threading contract (permanent for v4.x):** All methods must be called
 * on the main server thread. Enforced at the call site.
 *
 * Dependencies use internal port interfaces (DI Rule 1 — interfaces, not concretes):
 *  - [PriceRegistry]  — not [PriceServiceImpl]
 *  - [OrderBookPort]  — not [OrderBook]
 *
 * // No interface: TradeService IS the interface; this is the single impl.
 */
class TradeServiceImpl(
    private val registry:   PriceRegistry,
    private val orders:     OrderBookPort,
    private val tradeUC:    ExecuteTradeUseCase,
    private val openShortUC:  OpenShortUseCase,
    private val closeShortUC: CloseShortUseCase
) : TradeService {

    // ── Public API — pure delegation ──────────────────────────────────────────

    override fun executeBuy(player: Player, item: ItemKey, quantity: Int): TradeResult =
        tradeUC.executeBuy(player, item, quantity)

    override fun executeSell(player: Player, item: ItemKey, quantity: Int): TradeResult =
        tradeUC.executeSell(player, item, quantity)

    override fun placeLimitOrder(
        player: Player, item: ItemKey, quantity: Int,
        targetPrice: Double, isBuyOrder: Boolean
    ): OrderResult {
        if (!registry.isListed(item)) return OrderResult.NotListed
        return orders.place(player, item, quantity, targetPrice, isBuyOrder)
    }

    override fun cancelLimitOrder(player: Player, orderId: String): OrderResult =
        orders.cancel(player, orderId)

    override fun openShort(player: Player, item: ItemKey, quantity: Int): ShortResult =
        openShortUC.execute(player, item, quantity)

    override fun closeShort(player: Player, item: ItemKey): ShortResult =
        closeShortUC.execute(player, item)

    // ── Internal engine method — NOT in public TradeService API ───────────────

    /**
     * Process triggered limit orders for [item] at [currentPrice].
     * Called only by MarketTickTask on the main thread.
     */
    fun processTriggeredOrders(item: ItemKey, currentPrice: Double) {
        val triggered = orders.getTriggeredOrders(item, currentPrice)
        for (order in triggered) {
            val player = org.bukkit.Bukkit.getPlayer(order.playerUUID) ?: continue
            val result = if (order.isBuyOrder) executeBuy(player, item, order.quantity)
                         else                  executeSell(player, item, order.quantity)
            if (result is TradeResult.Success) orders.markFilled(order.orderId)
        }
        orders.pruneExpired(item)
    }
}
