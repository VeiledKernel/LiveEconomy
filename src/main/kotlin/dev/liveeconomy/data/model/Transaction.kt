package dev.liveeconomy.data.model

import dev.liveeconomy.api.item.ItemKey
import java.util.UUID

/**
 * An immutable record of a completed trade.
 *
 * Stored in the transaction log — one record per executed buy or sell.
 * Uses [ItemKey] for item identity.
 */
data class Transaction(
    val playerUuid: UUID,
    val timestamp:  Long,
    val item:       ItemKey,
    val action:     TradeAction,
    val quantity:   Int,
    val unitPrice:  Double,
    val total:      Double
) {
    /** Net value: positive for sells (received), negative for buys (spent). */
    val netValue: Double get() = when (action) {
        TradeAction.BUY  -> -total
        TradeAction.SELL ->  total
    }
}

/** Whether a transaction was a purchase, sale, or short position operation. */
enum class TradeAction {
    BUY,
    SELL,
    SHORT_OPEN,
    SHORT_CLOSE;

    val label: String get() = name.lowercase()
        .replace('_', ' ')
        .replaceFirstChar(Char::uppercase)
}
