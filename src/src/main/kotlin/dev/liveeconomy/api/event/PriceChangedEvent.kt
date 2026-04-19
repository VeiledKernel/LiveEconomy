package dev.liveeconomy.api.event

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI
import dev.liveeconomy.api.item.ItemKey

/**
 * Fired when an item's price changes during a market tick.
 * Not fired for every tiny fluctuation — only when the delta exceeds
 * the configured broadcast threshold.
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
data class PriceChangedEvent(
    val item:          ItemKey,
    val previousPrice: Double,
    val newPrice:      Double,
    val changePercent: Double
) : DomainEvent
