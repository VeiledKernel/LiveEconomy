package dev.liveeconomy.core.event

import dev.liveeconomy.api.event.DomainEvent
import dev.liveeconomy.api.event.DomainEventBus
import dev.liveeconomy.api.event.DomainEventHandler
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Synchronous [DomainEventBus] implementation.
 *
 * Dispatch model (Rule 13): publish() is synchronous — all handlers run and
 * complete before publish() returns. Handlers must not block or perform I/O.
 *
 * Thread safety: [CopyOnWriteArrayList] allows safe concurrent reads during
 * dispatch while subscribe/unsubscribe modify the list.
 */
class DomainEventBusImpl : DomainEventBus {

    private val handlers = CopyOnWriteArrayList<DomainEventHandler>()

    @Volatile private var running = false

    override fun start() {
        running = true
    }

    override fun stop() {
        running = false
        handlers.clear()
    }

    override fun subscribe(handler: DomainEventHandler) {
        if (!handlers.contains(handler)) handlers.add(handler)
    }

    override fun unsubscribe(handler: DomainEventHandler) {
        handlers.remove(handler)
    }

    /**
     * Dispatch [event] synchronously to all registered handlers.
     * Returns only after every handler has completed.
     * Exceptions in individual handlers are caught and logged — they do not
     * prevent subsequent handlers from running.
     */
    override fun publish(event: DomainEvent) {
        if (!running) return
        for (handler in handlers) {
            try {
                handler.handle(event)
            } catch (e: Exception) {
                // One bad handler must not block others
                System.err.println(
                    "[DomainEventBus] Handler ${handler::class.simpleName} " +
                    "threw on ${event::class.simpleName}: ${e.message}"
                )
            }
        }
    }
}
