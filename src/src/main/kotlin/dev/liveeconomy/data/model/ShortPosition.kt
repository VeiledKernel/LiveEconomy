package dev.liveeconomy.data.model

import dev.liveeconomy.api.item.ItemKey
import java.util.UUID

/**
 * An open short-sell position held by a player.
 *
 * Profit when price falls; loss when price rises.
 * P&L = (entryPrice - currentPrice) × quantity − fees.
 */
data class ShortPosition(
    val playerUUID:  UUID,
    val item:        ItemKey,
    val quantity:    Int,
    val entryPrice:  Double,
    val collateral:  Double
) {
    /** Unrealised P&L at [currentPrice]. Positive = profit, negative = loss. */
    fun unrealisedPnl(currentPrice: Double): Double =
        (entryPrice - currentPrice) * quantity

    /** Margin level as a percentage: (collateral / (currentPrice × qty)) × 100 */
    fun marginLevel(currentPrice: Double): Double {
        val exposure = currentPrice * quantity
        return if (exposure == 0.0) Double.MAX_VALUE
        else (collateral / exposure) * 100.0
    }
}
