package dev.liveeconomy.api.event

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI

/**
 * Fired when a market shock is applied to a category.
 *
 * @param shockType  human-readable shock identifier e.g. "mining", "boss_kill"
 * @param categoryId the affected market category ID
 * @param percent    shock magnitude — positive = price rise, negative = fall
 * @param message    optional broadcast message
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
data class ShockFiredEvent(
    val shockType:  String,
    val categoryId: String,
    val percent:    Double,
    val message:    String
) : DomainEvent
