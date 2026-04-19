package dev.liveeconomy.api.economy.result

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI

/**
 * Typed result of placing or cancelling a limit order.
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
sealed interface OrderResult {

    /** Order placed successfully. */
    data class Placed(
        /** Unique ID assigned to this order — use for cancellation. */
        val orderId: String
    ) : OrderResult

    /** Order cancelled successfully. */
    data object Cancelled : OrderResult

    /** Player has reached their maximum open order limit. */
    data object LimitReached : OrderResult

    /** No open order found with the given ID for this player. */
    data object NotFound : OrderResult

    /** Item is not registered on this market. */
    data object NotListed : OrderResult

    /** Order price or quantity was invalid (zero or negative). */
    data object InvalidParameters : OrderResult
}
