package dev.liveeconomy.api.event

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI
import dev.liveeconomy.api.item.ItemKey
import java.util.UUID

/**
 * Fired after a short position is successfully opened.
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
data class ShortOpenedEvent(
    val playerUuid: UUID,
    val item:       ItemKey,
    val quantity:   Int,
    val entryPrice: Double,
    val collateral: Double
) : DomainEvent
