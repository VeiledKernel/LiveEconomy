package dev.liveeconomy.boot

import dev.liveeconomy.api.storage.StorageProvider
import dev.liveeconomy.command.CommandFacade
import dev.liveeconomy.command.CommandRegistry
import org.bukkit.plugin.java.JavaPlugin

/**
 * Registers all commands with Bukkit.
 * Extracted from [dev.liveeconomy.PluginBoot] — pure delegation, no logic.
 */
object CommandRegistrar {
    fun register(plugin: JavaPlugin, cmd: CommandFacade, storage: StorageProvider) =
        CommandRegistry.register(plugin, cmd, storage)
}
