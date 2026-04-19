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
import dev.liveeconomy.core.usecase.port.PlayerResolver
import org.bukkit.entity.Player

/**
 * [TradeService] implementation — thin boundary, delegates to use cases.
 *
 * **No inventory or Bukkit logic here.** Business orchestration lives in:
 *  - [ExecuteTradeUseCase] — buy/sell flows (UUID-based, Bukkit-free)
 *  - [OpenShortUseCase]    — short position opening
 *  - [CloseShortUseCase]   — short position closing
 *
 * Passes [Player.uniqueId] to use cases rather than the Player object itself,
 * keeping `core/` free of Bukkit entity types in business flows.
 *
 * [PlayerResolver] is used ONLY in [processTriggeredOrders] — the one place
 * where a Bukkit player must be resolved from a UUID at runtime. This is an
 * accepted, documented platform boundary (DI-RULES.md Rule 5 exception context:
 * platform-layer task calls this method on the main thread, player resolution
 * is unavoidable at the trigger point).
 *
 * // No interface: TradeService IS the public interface; this is the single impl.
 */
class TradeServiceImpl(
    private val registry:     PriceRegistry,
    private val orders:       OrderBookPort,
    private val tradeUC:      ExecuteTradeUseCase,
    private val openShortUC:  OpenShortUseCase,
    private val closeShortUC: CloseShortUseCase,
    private val playerResolver: PlayerResolver
) : TradeService {

    // ── Public API — pass UUID to use cases, not Player ───────────────────────

    override fun executeBuy(player: Player, item: ItemKey, quantity: Int): TradeResult =
        tradeUC.executeBuy(player.uniqueId, item, quantity)

    override fun executeSell(player: Player, item: ItemKey, quantity: Int): TradeResult =
        tradeUC.executeSell(player.uniqueId, item, quantity)

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
        openShortUC.execute(player.uniqueId, item, quantity)

    override fun closeShort(player: Player, item: ItemKey): ShortResult =
        closeShortUC.execute(player.uniqueId, item)

    // ── Internal engine method — NOT in public TradeService API ───────────────

    /**
     * Process triggered limit orders for [item] at [currentPrice].
     * Called only by [dev.liveeconomy.platform.scheduler.MarketTickTask]
     * on the main thread.
     *
     * [PlayerResolver] is used here because limit orders store UUIDs and the
     * player must be resolved at execution time. This is the ONLY place in
     * core/ that requires player resolution — documented as an accepted
     * platform boundary (the caller, MarketTickTask, owns the thread context).
     */
    fun processTriggeredOrders(item: ItemKey, currentPrice: Double) {
        val triggered = orders.getTriggeredOrders(item, currentPrice)
        for (order in triggered) {
            // PlayerResolver keeps Bukkit.getPlayer() out of this class body
            val online = playerResolver.isOnline(order.playerUUID)
            if (!online) continue

            val result = if (order.isBuyOrder)
                tradeUC.executeBuy(order.playerUUID, item, order.quantity)
            else
                tradeUC.executeSell(order.playerUUID, item, order.quantity)

            if (result is TradeResult.Success) orders.markFilled(order.orderId)
        }
        orders.pruneExpired(item)
    }
}
