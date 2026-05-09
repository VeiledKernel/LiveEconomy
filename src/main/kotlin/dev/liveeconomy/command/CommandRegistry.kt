package dev.liveeconomy.command

import dev.liveeconomy.api.storage.StorageProvider
import dev.liveeconomy.command.admin.*
import dev.liveeconomy.command.framework.CommandNode
import dev.liveeconomy.command.market.*
import dev.liveeconomy.command.trading.*
import org.bukkit.plugin.java.JavaPlugin

/**
 * Assembles and registers all [CommandNode] instances with Bukkit.
 *
 * Called once from [dev.liveeconomy.PluginBoot.start].
 * No business logic here — pure wiring.
 *
 * Each root command maps to a [CommandNode] with its subcommands.
 * The default action (no subcommand) opens the relevant GUI.
 */
object CommandRegistry {

    fun register(plugin: JavaPlugin, cmd: CommandFacade, storage: StorageProvider) {

        // /market [role|search|stats]
        plugin.getCommand("market")?.let { bc ->
            val node = CommandNode(
                rootPermission = "liveeconomy.market",
                defaultAction  = { sender, _ ->
                    val player = sender as? org.bukkit.entity.Player ?: return@CommandNode
                    cmd.gui.market().open(player)
                },
                subCommands = listOf(
                    MarketRoleCommand(cmd),
                    MarketSearchCommand(cmd),
                    MarketStatsCommand(cmd)
                )
            )
            bc.setExecutor(node)
            bc.tabCompleter = node
        }

        // /wallet
        plugin.getCommand("wallet")?.let { bc ->
            val node = CommandNode(
                rootPermission = "liveeconomy.wallet",
                defaultAction  = { sender, _ ->
                    val player = sender as? org.bukkit.entity.Player ?: return@CommandNode
                    cmd.gui.wallet().open(player)
                },
                subCommands = emptyList()
            )
            bc.setExecutor(node)
            bc.tabCompleter = node
        }

        // /portfolio
        plugin.getCommand("portfolio")?.let { bc ->
            val node = CommandNode(
                rootPermission = "liveeconomy.portfolio",
                defaultAction  = { sender, _ ->
                    val player = sender as? org.bukkit.entity.Player ?: return@CommandNode
                    cmd.gui.portfolio().open(player)
                },
                subCommands = emptyList()
            )
            bc.setExecutor(node)
            bc.tabCompleter = node
        }

        // /invest [alerts|alert <item> <price> <above|below>|prestige]
        plugin.getCommand("invest")?.let { bc ->
            val node = CommandNode(
                rootPermission = "liveeconomy.invest",
                defaultAction  = { sender, _ ->
                    val player = sender as? org.bukkit.entity.Player ?: return@CommandNode
                    cmd.gui.priceAlerts().open(player)
                },
                subCommands = listOf(
                    InvestAlertsCommand(cmd),
                    InvestAlertCommand(cmd),
                    InvestPrestigeCommand(cmd)
                )
            )
            bc.setExecutor(node)
            bc.tabCompleter = node
        }

        // /short <open|close|list>
        plugin.getCommand("short")?.let { bc ->
            val node = CommandNode(
                rootPermission = "liveeconomy.vip.short",
                defaultAction  = null,
                subCommands = listOf(
                    ShortOpenCommand(cmd),
                    ShortCloseCommand(cmd),
                    ShortListCommand(cmd)
                )
            )
            bc.setExecutor(node)
            bc.tabCompleter = node
        }

        // /leconomy <reload|shock|setprice|save|bull|crash>
        plugin.getCommand("leconomy")?.let { bc ->
            val node = CommandNode(
                rootPermission = "liveeconomy.admin",
                defaultAction  = null,
                subCommands = listOf(
                    AdminReloadCommand(cmd),
                    AdminSetPriceCommand(cmd),
                    AdminShockCommand(cmd),
                    AdminSaveCommand(cmd, storage),
                    AdminBullCommand(cmd),
                    AdminCrashCommand(cmd)
                )
            )
            bc.setExecutor(node)
            bc.tabCompleter = node
        }
    }
}
