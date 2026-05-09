package dev.liveeconomy.command.framework

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Root of a command tree. Registered with Bukkit as both executor and tab completer.
 *
 * Dispatches to [SubCommand] children by matching args[0] case-insensitively.
 * If no subcommand is provided, or the args match nothing, [defaultAction] is called.
 *
 * Usage: register via [org.bukkit.command.PluginCommand.setExecutor] in PluginBoot.
 *
 * @param rootPermission if set, sender must have this permission for any subcommand.
 * @param defaultAction runs when no subcommand arg is provided (e.g. open a GUI).
 */
class CommandNode(
    private val rootPermission: String? = null,
    private val defaultAction:  ((CommandSender, Array<String>) -> Unit)? = null,
    private val subCommands:    List<SubCommand>
) : CommandExecutor, TabCompleter {

    private val byName: Map<String, SubCommand> =
        subCommands.associateBy { it.name.lowercase() }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        // Root permission gate
        if (rootPermission != null && !sender.hasPermission(rootPermission)) {
            sender.sendMessage("§cYou don't have permission to use this command.")
            return true
        }

        // No subcommand → default action
        if (args.isEmpty()) {
            defaultAction?.invoke(sender, args) ?: sendHelp(sender)
            return true
        }

        val sub = byName[args[0].lowercase()]

        if (sub == null) {
            sendHelp(sender)
            return true
        }

        // Subcommand-level permission
        if (!sub.checkPermission(sender)) return true

        // Player-only gate
        if (sub.playerOnly && sender !is Player) {
            sender.sendMessage("§cThis subcommand requires a player.")
            return true
        }

        val handled = sub.execute(sender, args.drop(1).toTypedArray())
        if (!handled) sender.sendMessage("§7Usage: /${command.name} ${sub.name} ${sub.usage}")
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): List<String> {
        if (rootPermission != null && !sender.hasPermission(rootPermission)) return emptyList()

        return when {
            args.size <= 1 -> {
                // Complete the subcommand name
                val prefix = args.getOrElse(0) { "" }.lowercase()
                subCommands
                    .filter { sub ->
                        sub.name.startsWith(prefix) &&
                        (sub.permission == null || sender.hasPermission(sub.permission!!))
                    }
                    .map { it.name }
            }
            else -> {
                // Delegate to the subcommand
                val sub = byName[args[0].lowercase()] ?: return emptyList()
                if (sub.permission != null && !sender.hasPermission(sub.permission!!)) return emptyList()
                sub.tabComplete(sender, args.drop(1).toTypedArray())
            }
        }
    }

    private fun sendHelp(sender: CommandSender) {
        val visible = subCommands.filter { sub ->
            sub.permission == null || sender.hasPermission(sub.permission!!)
        }
        if (visible.isEmpty()) {
            sender.sendMessage("§cNo subcommands available.")
            return
        }
        sender.sendMessage("§6§lLiveEconomy §7— Available subcommands:")
        visible.forEach { sub ->
            sender.sendMessage("  §e${sub.name} §7${sub.usage} §8— ${sub.description}")
        }
    }
}
