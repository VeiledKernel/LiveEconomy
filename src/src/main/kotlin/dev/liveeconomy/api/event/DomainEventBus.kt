package dev.liveeconomy.api.event

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI
import dev.liveeconomy.platform.Lifecycle

/**
 * Internal synchronous event bus for LiveEconomy domain events.
 *
 * Extends [Lifecycle] — must be started before use and stopped on
 * plugin shutdown to release all subscribers and prevent memory leaks (Rule 6).
 *
 * **Dispatch model (Rule 13):** Synchronous. All handlers run and complete
 * before [publish] returns. Handlers must not block.
 *
 * **Memory safety:** Always [unsubscribe] handlers when they are no longer
 * needed, particularly for per-player or per-session objects.
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
interface DomainEventBus : Lifecycle {

    /**
     * Publish [event] to all currently subscribed handlers.
     * Dispatches synchronously — returns only after all handlers complete.
     */
    fun publish(event: DomainEvent)

    /**
     * Register [handler] to receive all future events.
     * Handlers are called in registration order.
     * Registering the same handler twice is a no-op.
     */
    fun subscribe(handler: DomainEventHandler)

    /**
     * Remove [handler] from future event dispatch.
     * Safe to call with a handler that was never registered.
     */
    fun unsubscribe(handler: DomainEventHandler)
}
