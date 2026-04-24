package dev.liveeconomy

import dev.liveeconomy.api.Lifecycle
import dev.liveeconomy.platform.config.ConfigLoader
import dev.liveeconomy.platform.scheduler.SchedulerImpl
import org.bukkit.plugin.java.JavaPlugin

/**
 * LiveEconomy v4.0 — Entry point.
 *
 * Delegates all wiring to [PluginBoot] — this file is intentionally thin.
 * Contains only plugin lifecycle hooks and the scheduler construction.
 */
class LiveEconomy : JavaPlugin() {

    private lateinit var boot: PluginBoot

    override fun onEnable() {
        val scheduler = SchedulerImpl(this)
        val configs   = ConfigLoader(this).load()
        boot = PluginBoot(this, scheduler, configs)
        boot.start()
        logger.info("[LiveEconomy] v4.0 enabled. Storage: ${configs.storage.type}")
    }

    override fun onDisable() {
        if (::boot.isInitialized) boot.stop()
        logger.info("[LiveEconomy] Disabled.")
    }
}
