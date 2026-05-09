package dev.liveeconomy.boot

import dev.liveeconomy.api.Lifecycle
import dev.liveeconomy.data.config.EconomyConfig
import dev.liveeconomy.integration.essentials.EssentialsSellBlocker
import dev.liveeconomy.integration.nexo.NexoIntegration
import dev.liveeconomy.integration.papi.LiveEconomyExpansion
import org.bukkit.plugin.java.JavaPlugin

/**
 * Detects, instantiates, and registers all optional external integrations.
 *
 * Extracted from [dev.liveeconomy.PluginBoot] — pure orchestration, no logic.
 *
 * Returns [IntegrationSet] containing all resolved integrations.
 * Callers add lifecycle-implementing integrations to the lifecycle list.
 * Absent optional plugins are logged and skipped — never crash boot.
 */
class IntegrationRegistrar(private val plugin: JavaPlugin) {

    data class IntegrationSet(
        val nexo:         NexoIntegration,
        val papiExpansion: LiveEconomyExpansion?,     // null if PAPI absent
        val essentials:   EssentialsSellBlocker?      // null if Essentials absent
    ) {
        /** All integrations that implement [Lifecycle], for easy bulk registration. */
        val lifecycles: List<Lifecycle>
            get() = buildList {
                add(nexo)
                papiExpansion?.let { add(it) }
                essentials?.let { add(it) }
            }
    }

    fun register(config: EconomyConfig): IntegrationSet {
        val server = plugin.server

        // Nexo — always created; enabled flag reflects actual availability
        val nexo = NexoIntegration.detect(server).also { integration ->
            if (integration.enabled)
                plugin.logger.info("[LiveEconomy] Nexo detected — custom item support enabled.")
            else
                plugin.logger.info("[LiveEconomy] Nexo not found — vanilla items only.")
        }

        // PlaceholderAPI — optional, register expansion if present
        val papiExpansion = if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            plugin.logger.info("[LiveEconomy] PlaceholderAPI detected — registering placeholders.")
            LiveEconomyExpansion()
        } else {
            plugin.logger.info("[LiveEconomy] PlaceholderAPI not found — placeholders disabled.")
            null
        }

        // Essentials — optional, block /sell if configured
        val essentials = EssentialsSellBlocker.detectOrNull(plugin, config).also { blocker ->
            if (blocker != null)
                plugin.logger.info("[LiveEconomy] Essentials detected — /sell blocker active (config: block-essentials-sell).")
            else
                plugin.logger.info("[LiveEconomy] Essentials not found — /sell blocker skipped.")
        }

        return IntegrationSet(nexo, papiExpansion, essentials)
    }
}
