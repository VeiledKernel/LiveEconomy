package dev.liveeconomy.api.extension

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI
import dev.liveeconomy.api.event.DomainEvent

/**
 * Extension point for market shock triggers.
 *
 * Each shock type is one file implementing this interface.
 * Registered via [dev.liveeconomy.core.event.shock.ShockRegistry].
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
fun interface ShockHandler {
    /**
     * Evaluate [event] and apply a market shock if conditions are met.
     * Must be fast — called on the main thread for every Bukkit event.
     */
    fun handle(event: DomainEvent)
}
