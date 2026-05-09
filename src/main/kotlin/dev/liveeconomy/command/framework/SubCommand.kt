package dev.liveeconomy.command.framework

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * A single node in the command tree.
 *
 * Each subcommand declares:
 *  - its [name] (matched case-insensitively against args[0])
 *  - optional [permission] required to execute or see it in tab complete
 *  - whether it requires a [playerOnly] sender
 *  - [execute] — the action
 *  - [tabComplete] — suggestions for the next argument
 *
 * SubCommands are stateless value objects — all mutable state lives in services.
 */
interface SubCommand {
    val name:        String
    val usage:       String       // e.g. "<item> <price> <above|below>"
    val description: String
    val permission:  String?      // null = no permission required
    val playerOnly:  Boolean      get() = true

    /**
     * Execute this subcommand.
     * [args] does NOT include the subcommand name itself — it's already consumed.
     * Returns true if the command was handled (even on error), false to print usage.
     */
    fun execute(sender: CommandSender, args: Array<String>): Boolean

    /**
     * Tab-complete suggestions for the next argument after the subcommand name.
     * [args] is the remaining args, not including the subcommand name.
     */
    fun tabComplete(sender: CommandSender, args: Array<String>): List<String> = emptyList()
}

/** Convenience — casts sender to Player or sends an error and returns null. */
fun SubCommand.requirePlayer(sender: CommandSender): Player? {
    if (sender !is Player) { sender.sendMessage("§cThis command requires a player."); return null }
    return sender
}

/** Convenience — checks permission and sends an error if denied. */
fun SubCommand.checkPermission(sender: CommandSender): Boolean {
    val perm = permission ?: return true
    if (sender.hasPermission(perm)) return true
    sender.sendMessage("§cYou don't have permission to do that.")
    return false
}
