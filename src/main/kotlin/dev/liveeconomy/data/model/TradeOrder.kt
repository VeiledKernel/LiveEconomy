package dev.liveeconomy.data.model

import dev.liveeconomy.api.item.ItemKey
import java.time.Instant
import java.util.UUID

/**
 * A pending limit order — buy or sell at a specific target price.
 * Expires after [expiryHours] hours if not filled.
 *
 * Uses [ItemKey] for item identity — no direct Material dependency.
 */
data class TradeOrder(
    /** Stable unique ID — used for cancellation and storage keying. */
    val orderId:     String = UUID.randomUUID().toString(),
    val playerUUID:  UUID,
    val playerName:  String,
    val item:        ItemKey,
    val quantity:    Int,
    val targetPrice: Double,
    val isBuyOrder:  Boolean,
    val placedAt:    Instant,
    val expiryHours: Long = 24L
) {
    val isExpired: Boolean
        get() = Instant.now().isAfter(placedAt.plusSeconds(expiryHours * 3600))

    val typeLabel: String
        get() = if (isBuyOrder) "Buy limit" else "Sell limit"

    /**
     * True when the current market price should trigger this order.
     * Buy orders fill when price drops to or below target.
     * Sell orders fill when price rises to or above target.
     */
    fun shouldTrigger(currentPrice: Double): Boolean =
        if (isBuyOrder) currentPrice <= targetPrice
        else            currentPrice >= targetPrice
}
