package dev.liveeconomy.platform

/**
 * Lifecycle contract for any component that starts background work,
 * holds an open connection, or maintains ongoing state.
 *
 * **Required for (Rule 6):**
 * - All `*Task` scheduler classes
 * - [dev.liveeconomy.platform.scheduler.AsyncTradeQueue]
 * - [dev.liveeconomy.api.event.DomainEventBus]
 * - All [dev.liveeconomy.api.storage.StorageProvider] implementations
 * - Integration adapters that open connections (Vault, Nexo, etc.)
 *
 * **Shutdown order (onDisable):** reverse of startup — tasks first,
 * storage last. See composition root in `LiveEconomy.kt`.
 */
interface Lifecycle {

    /**
     * Start this component. Called once during plugin startup.
     * Must be idempotent — calling start() twice is safe.
     */
    fun start()

    /**
     * Stop this component and release all resources.
     * Called during plugin shutdown or reload.
     * Must be idempotent — calling stop() twice is safe.
     */
    fun stop()
}
