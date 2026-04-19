package dev.liveeconomy.api.event

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.data.model.TradeAction
import java.util.UUID

/**
 * Fired after a trade executes successfully.
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
data class TradeExecutedEvent(
    val playerUuid: UUID,
    val item:       ItemKey,
    val action:     TradeAction,
    val quantity:   Int,
    val unitPrice:  Double,
    val total:      Double,
    val taxPaid:    Double,
    val newPrice:   Double
) : DomainEvent
