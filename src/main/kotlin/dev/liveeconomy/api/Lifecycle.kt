package dev.liveeconomy.api

/**
 * Lifecycle contract for any component that starts background work,
 * holds an open connection, or maintains ongoing state.
 *
 * Lives in `api/` because it is a domain contract — it defines how
 * LiveEconomy manages component lifetimes regardless of platform.
 * `platform/` implements it; it does not own it.
 *
 * **Required for (Rule 6):**
 * - All `*Task` scheduler classes
 * - [dev.liveeconomy.api.event.DomainEventBus]
 * - All [dev.liveeconomy.api.storage.StorageProvider] implementations
 * - Integration adapters that open connections
 *
 * **Shutdown order:** reverse of startup — tasks first, storage last.
 *
 * @since 4.0
 */
interface Lifecycle {

    /**
     * Start this component. Called once during plugin startup.
     * Idempotent — calling [start] twice must be safe.
     */
    fun start()

    /**
     * Stop this component and release all resources.
     * Called during plugin shutdown or reload.
     * Idempotent — calling [stop] twice must be safe.
     */
    fun stop()
}
