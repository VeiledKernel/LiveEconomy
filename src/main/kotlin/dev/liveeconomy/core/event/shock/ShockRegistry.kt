package dev.liveeconomy.core.event.shock

import dev.liveeconomy.api.event.DomainEvent
import dev.liveeconomy.api.extension.ShockHandler

/**
 * Registry of all active [ShockHandler] implementations.
 *
 * Adding a new shock type = new file implementing [ShockHandler] + entry here.
 * Nothing else changes.
 *
 * // No interface: registry pattern, single impl.
 */
class ShockRegistry(private val handlers: List<ShockHandler>) {
    fun dispatch(event: DomainEvent) = handlers.forEach { it.handle(event) }
    fun count(): Int = handlers.size
}
