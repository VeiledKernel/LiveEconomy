package dev.liveeconomy.api.economy

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI
import dev.liveeconomy.api.economy.result.OrderResult
import dev.liveeconomy.api.economy.result.ShortResult
import dev.liveeconomy.api.economy.result.TradeResult
import dev.liveeconomy.api.item.ItemKey
import org.bukkit.entity.Player

/**
 * Write operations for the LiveEconomy market.
 *
 * All methods MUST be called on the main thread — they touch player
 * inventory and economy balances which are Bukkit-unsafe off-thread.
 * Implementations must enforce this via Paper's thread checks.
 *
 * Internal engine methods (processLimitOrders, tick updates, shock
 * application) are NOT part of this interface — they are internal
 * orchestration concerns.
 *
 * Inject this interface when you need to execute trades:
 * ```kotlin
 * class BuyCommand(private val trade: TradeService)
 * ```
 *
 * Access via:
 * ```kotlin
 * val trade = LiveEconomyAPI.get().trade()
 * ```
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
interface TradeService {

    /**
     * Execute an immediate market buy for [player].
     *
     * Deducts cost from the player's balance (via Vault or internal wallet),
     * delivers [quantity] units of [item] to the player's inventory,
     * and applies supply/demand price impact.
     *
     * Must be called on the main thread.
     */
    fun executeBuy(
        player:   Player,
        item:     ItemKey,
        quantity: Int
    ): TradeResult

    /**
     * Execute an immediate market sell for [player].
     *
     * Removes [quantity] units of [item] from the player's inventory,
     * deposits sale revenue to the player's balance,
     * and applies supply/demand price impact.
     *
     * Must be called on the main thread.
     */
    fun executeSell(
        player:   Player,
        item:     ItemKey,
        quantity: Int
    ): TradeResult

    /**
     * Place a limit order that will execute when [item]'s price reaches [targetPrice].
     *
     * Buy orders: execute when price falls to or below [targetPrice].
     * Sell orders: execute when price rises to or above [targetPrice].
     *
     * The order persists across server restarts (if SQL backend is configured).
     * Returns [OrderResult.Placed] with a stable order ID for later cancellation.
     */
    fun placeLimitOrder(
        player:      Player,
        item:        ItemKey,
        quantity:    Int,
        targetPrice: Double,
        isBuyOrder:  Boolean
    ): OrderResult

    /**
     * Cancel an open limit order by its [orderId].
     *
     * Only the player who placed the order may cancel it.
     * Returns [OrderResult.NotFound] if no matching order exists.
     */
    fun cancelLimitOrder(
        player:  Player,
        orderId: String
    ): OrderResult

    /**
     * Open a short-sell position on [item] for [player].
     *
     * Locks collateral equal to (price × quantity × collateralRatio) from
     * the player's balance. Profit is realised when the position is closed
     * at a lower price.
     *
     * Requires short selling to be enabled in config and the player to hold
     * the `liveeconomy.vip.short` permission (if VIP mode is active).
     */
    fun openShort(
        player:   Player,
        item:     ItemKey,
        quantity: Int
    ): ShortResult

    /**
     * Close an open short position on [item] for [player].
     *
     * Releases the collateral and settles P&L into the player's balance.
     * Returns [ShortResult.NoPosition] if the player has no open short on [item].
     */
    fun closeShort(
        player: Player,
        item:   ItemKey
    ): ShortResult
}
