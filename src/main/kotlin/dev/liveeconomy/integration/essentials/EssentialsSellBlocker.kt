package dev.liveeconomy.integration.essentials

import dev.liveeconomy.api.Lifecycle
import dev.liveeconomy.data.config.EconomyConfig
import dev.liveeconomy.util.ChatUtil
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * Blocks Essentials `/sell` commands when [EconomyConfig.blockEssentialsSell] is true.
 *
 * Prevents economy conflicts where Essentials' sell system bypasses LiveEconomy's
 * pricing engine and lets players sell items at static Essentials prices.
 *
 * **Boot safety:** only registered if Essentials is present AND config enables it.
 * Plugin boots normally when Essentials is absent.
 *
 * **No business logic:** only intercepts a command and cancels it with a message.
 * Pricing, inventory, and wallet logic lives in core services.
 *
 * Implements [Lifecycle] so [PluginBoot]'s lifecycle list handles registration.
 */
class EssentialsSellBlocker(
    private val plugin: JavaPlugin,
    private val config: EconomyConfig
) : Listener, Lifecycle {

    private val blockedCommands = setOf(
        "sell", "essentials:sell", "es:sell"
    )

    override fun start() {
        if (!config.blockEssentialsSell) return
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    override fun stop() {
        // Bukkit unregisters all plugin listeners on disable — no manual cleanup needed.
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        if (!config.blockEssentialsSell) return

        val command = event.message
            .trimStart('/')
            .split(" ")
            .firstOrNull()
            ?.lowercase()
            ?: return

        if (command in blockedCommands) {
            event.isCancelled = true
            event.player.sendMessage(
                "${ChatUtil.prefix()}§cThe §f/sell §ccommand is disabled. " +
                "Use §e/market §cto trade items through the live economy."
            )
        }
    }

    companion object {
        /**
         * Returns an [EssentialsSellBlocker] only if Essentials is installed.
         * Returns null when Essentials is absent — caller skips registration.
         */
        fun detectOrNull(plugin: JavaPlugin, config: EconomyConfig): EssentialsSellBlocker? {
            val essentials = plugin.server.pluginManager.getPlugin("Essentials") ?: return null
            if (!essentials.isEnabled) return null
            return EssentialsSellBlocker(plugin, config)
        }
    }
}
