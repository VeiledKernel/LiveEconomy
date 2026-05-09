package dev.liveeconomy.boot

import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.api.player.PortfolioService
import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.core.alert.AlertService
import dev.liveeconomy.core.event.shock.*
import dev.liveeconomy.data.config.EventsConfig
import dev.liveeconomy.gui.framework.MenuManager
import dev.liveeconomy.platform.listener.PlayerListener
import dev.liveeconomy.platform.listener.ShockListener
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

object ListenerRegistrar {

    fun register(plugin: JavaPlugin, menuManager: MenuManager) {
        plugin.server.pluginManager.registerEvents(menuManager, plugin)
    }

    fun registerShockListener(
        plugin: JavaPlugin, mining: MiningShock, harvest: HarvestShock,
        fishing: FishingShock, enchanting: EnchantingShock, bossKill: BossKillShock,
        raid: RaidShock, mapper: ItemKeyMapper, config: EventsConfig
    ) {
        plugin.server.pluginManager.registerEvents(
            ShockListener(mining, harvest, fishing, enchanting, bossKill, raid, mapper, config), plugin)
        plugin.logger.info("[LiveEconomy] ShockListener registered.")
    }

    fun registerPlayerListener(plugin: JavaPlugin, wallet: WalletService,
                                portfolio: PortfolioService, alerts: AlertService) {
        plugin.server.pluginManager.registerEvents(PlayerListener(wallet, portfolio, alerts), plugin)
        plugin.logger.info("[LiveEconomy] PlayerListener registered.")
    }

    fun registerAll(plugin: JavaPlugin, vararg listeners: Listener) =
        listeners.forEach { plugin.server.pluginManager.registerEvents(it, plugin) }
}
