package dev.liveeconomy.api.event

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI

/**
 * Base type for all internal LiveEconomy domain events.
 *
 * Domain events are dispatched synchronously on the calling thread
 * (Rule 13 — bus is synchronous, deterministic, testable).
 * Heavy work inside handlers must be delegated to [dev.liveeconomy.api.scheduler.Scheduler].
 *
 * External plugins receive these via Bukkit events after
 * [dev.liveeconomy.core.event.BukkitEventBridge] re-publishes them.
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
sealed interface DomainEvent
