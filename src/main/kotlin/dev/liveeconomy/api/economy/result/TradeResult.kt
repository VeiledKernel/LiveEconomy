package dev.liveeconomy.api.economy.result

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI

/**
 * Typed result of a market buy or sell execution.
 *
 * Callers must exhaustively handle all cases:
 * ```kotlin
 * when (val result = trade.executeBuy(player, item, qty)) {
 *     is TradeResult.Success          -> showReceipt(result.total, result.newPrice)
 *     is TradeResult.InsufficientFunds -> player.sendMessage("Not enough money")
 *     is TradeResult.InsufficientItems -> player.sendMessage("Not enough items")
 *     is TradeResult.NotListed         -> player.sendMessage("Item not on market")
 *     is TradeResult.Cooldown          -> player.sendMessage("Trade too fast")
 *     is TradeResult.ShortSellDisabled -> player.sendMessage("Short selling off")
 *     is TradeResult.NoInventorySpace  -> player.sendMessage("Inventory full")
 * }
 * ```
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
sealed interface TradeResult {

    /** Trade executed successfully. */
    data class Success(
        /** Total currency amount exchanged (after tax). */
        val total:    Double,
        /** Market price after this trade's impact. */
        val newPrice: Double,
        /** Tax amount deducted from the transaction. */
        val taxPaid:  Double
    ) : TradeResult

    /** Player cannot afford the purchase. */
    data object InsufficientFunds : TradeResult

    /** Player does not hold enough items to sell. */
    data object InsufficientItems : TradeResult

    /** Item is not registered on this market. */
    data object NotListed : TradeResult

    /** Player triggered the per-player trade cooldown. */
    data object Cooldown : TradeResult

    /** Short selling is disabled on this server. */
    data object ShortSellDisabled : TradeResult

    /** Player's inventory has no space to receive purchased items. */
    data object NoInventorySpace : TradeResult
}
