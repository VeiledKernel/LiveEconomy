package dev.liveeconomy.api.event

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI

/**
 * Subscriber interface for [DomainEventBus].
 *
 * Implementations must be fast — the event bus is synchronous (Rule 13).
 * Delegate any heavy work (DB writes, HTTP calls) to
 * [dev.liveeconomy.api.scheduler.Scheduler.runAsync].
 *
 * Registration:
 * ```kotlin
 * domainBus.subscribe(MyHandler())
 * ```
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
fun interface DomainEventHandler {
    /**
     * Called synchronously when an event is published.
     * Must not block, sleep, or perform heavy I/O.
     */
    fun handle(event: DomainEvent)
}
