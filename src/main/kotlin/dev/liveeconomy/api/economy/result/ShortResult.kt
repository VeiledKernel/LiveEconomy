package dev.liveeconomy.api.economy.result

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI

/**
 * Typed result of opening or closing a short-sell position.
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
sealed interface ShortResult {

    /** Short position opened successfully. */
    data class Opened(
        /** Collateral locked for this position. */
        val collateral:  Double,
        /** Market entry price at time of opening. */
        val entryPrice:  Double
    ) : ShortResult

    /** Short position closed successfully. */
    data class Closed(
        /** Realised profit/loss. Positive = profit, negative = loss. */
        val pnl: Double
    ) : ShortResult

    /** Short selling is disabled on this server. */
    data object Disabled : ShortResult

    /** Item is not registered on this market. */
    data object NotListed : ShortResult

    /** Player has no open short position on this item. */
    data object NoPosition : ShortResult

    /** Player has insufficient funds to cover the required collateral. */
    data object InsufficientCollateral : ShortResult

    /** Player already has an open short on this item. */
    data object AlreadyOpen : ShortResult
}
