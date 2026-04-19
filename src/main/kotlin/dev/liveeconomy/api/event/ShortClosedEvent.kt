package dev.liveeconomy.api.event

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI
import dev.liveeconomy.api.item.ItemKey
import java.util.UUID

/**
 * Fired after a short position is successfully closed.
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
data class ShortClosedEvent(
    val playerUuid: UUID,
    val item:       ItemKey,
    val quantity:   Int,
    val exitPrice:  Double,
    val pnl:        Double
) : DomainEvent
