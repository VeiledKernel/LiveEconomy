package dev.liveeconomy.integration.nexo

import dev.liveeconomy.api.Lifecycle
import dev.liveeconomy.api.item.ItemKey

/**
 * Nexo custom item integration.
 *
 * Centralises all Nexo API calls — no other class may import Nexo APIs.
 * Core resolves custom items through [dev.liveeconomy.api.item.ItemKeyMapper],
 * which delegates to this class when the `nexo:` namespace is encountered.
 *
 * **Boot safety:** [enabled] is false if Nexo is not installed. All methods
 * return null or safe defaults when disabled. Plugin boots normally without Nexo.
 *
 * **Lifecycle:** implements [Lifecycle] so future Nexo event hooks can be wired
 * through [dev.liveeconomy.PluginBoot]'s lifecycle list.
 */
class NexoIntegration(private val nexoAvailable: Boolean) : Lifecycle {

    val enabled: Boolean get() = nexoAvailable

    override fun start() {
        if (!enabled) return
        // Future: register Nexo item event listeners here
    }

    override fun stop() {
        // Future: unregister Nexo listeners here
    }

    /**
     * Resolves a Nexo item ID string to an [ItemKey].
     * Returns null (rather than throwing) when Nexo is absent, so callers
     * can fall back gracefully without try/catch boilerplate.
     *
     * @param nexoId the raw Nexo item ID, e.g. `"ruby"` (no namespace prefix)
     */
    fun resolve(nexoId: String): ItemKey? {
        if (!enabled) return null
        return runCatching {
            // Nexo API lookup — wrapped so a bad ID doesn't crash the call site
            val item = com.nexomc.nexo.api.NexoItems.itemFromId(nexoId) ?: return null
            dev.liveeconomy.core.item.NexoItemKey(namespace = "nexo", key = nexoId)
        }.getOrNull()
    }

    /**
     * Returns true if a Nexo item with this ID exists.
     * Used by [dev.liveeconomy.platform.item.BukkitItemKeyMapper] for validation.
     */
    fun exists(nexoId: String): Boolean {
        if (!enabled) return false
        return runCatching {
            com.nexomc.nexo.api.NexoItems.itemFromId(nexoId) != null
        }.getOrDefault(false)
    }

    companion object {
        /** Creates an [NexoIntegration] by detecting whether Nexo is loaded. */
        fun detect(server: org.bukkit.Server): NexoIntegration =
            NexoIntegration(server.pluginManager.getPlugin("Nexo") != null)
    }
}
