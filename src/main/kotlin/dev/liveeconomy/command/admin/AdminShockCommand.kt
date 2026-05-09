package dev.liveeconomy.command.admin

import dev.liveeconomy.command.framework.CompletionEngine
import dev.liveeconomy.command.CommandFacade
import dev.liveeconomy.command.framework.SubCommand
import dev.liveeconomy.util.ChatUtil
import org.bukkit.command.CommandSender

/** /leconomy shock <item> <percent> [message] — manually fires a price shock */
class AdminShockCommand(private val cmd: CommandFacade) : SubCommand {
    override val name        = "shock"
    override val usage       = "<item> <percent> [message]"
    override val description = "Manually fire a price shock"
    override val permission  = "liveeconomy.admin"
    override val playerOnly  = false

    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size < 2) return false

        val item = cmd.economy.query.findItemById(args[0]) ?: run {
            sender.sendMessage("${ChatUtil.prefix()}§cItem §f${args[0]} §cnot found.")
            return true
        }
        val pct = args[1].toDoubleOrNull() ?: run {
            sender.sendMessage("${ChatUtil.prefix()}§cPercent must be a number (e.g. §f-10 §cor §f15§c).")
            return true
        }
        val message = if (args.size > 2) args.drop(2).joinToString(" ") else null

        cmd.economy.price.applyShock(item.itemKey, pct, message ?: "Admin shock")
        val sign = if (pct >= 0) "§a+" else "§c"
        sender.sendMessage("${ChatUtil.prefix()}§7Shock applied: §f${item.itemKey.displayName()} $sign${pct}%§7.")
        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> =
        if (args.size == 1) CompletionEngine.marketItems(cmd.economy.query, args[0])
        else emptyList()
}
