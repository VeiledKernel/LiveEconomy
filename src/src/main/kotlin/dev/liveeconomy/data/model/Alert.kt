package dev.liveeconomy.data.model

import dev.liveeconomy.api.item.ItemKey
import java.util.UUID

/**
 * A price alert set by a player.
 *
 * Fires when [item]'s market price crosses [targetPrice] in [direction].
 * Uses [ItemKey] for item identity — no direct Material dependency.
 */
data class Alert(
    val playerUuid:  UUID,
    val item:        ItemKey,
    val targetPrice: Double,
    val direction:   Direction
) {
    /** Returns true if [currentPrice] satisfies this alert's condition. */
    fun isTriggered(currentPrice: Double): Boolean = when (direction) {
        Direction.ABOVE -> currentPrice >= targetPrice
        Direction.BELOW -> currentPrice <= targetPrice
    }
}

/** Direction of a price alert relative to the target price. */
enum class Direction {
    /** Alert fires when price rises to or above target. */
    ABOVE,

    /** Alert fires when price falls to or below target. */
    BELOW;

    companion object {
        fun fromString(value: String): Direction? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}
