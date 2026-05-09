package dev.liveeconomy.command.admin

import dev.liveeconomy.command.CommandFacade
import dev.liveeconomy.command.framework.SubCommand
import dev.liveeconomy.util.ChatUtil
import org.bukkit.command.CommandSender

/** /leconomy bull and /leconomy crash — market-wide shocks */
class AdminBullCommand(private val cmd: CommandFacade) : SubCommand {
    override val name        = "bull"
    override val usage       = "[percent]"
    override val description = "Fire a server-wide bull market shock"
    override val permission  = "liveeconomy.admin"
    override val playerOnly  = false

    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val pct = args.getOrNull(0)?.toDoubleOrNull() ?: 10.0
        applyToAll(cmd, pct, "§a§lBull market!")
        sender.sendMessage("${ChatUtil.prefix()}§a§lBull market fired (+${pct}% on all items).")
        return true
    }
}

class AdminCrashCommand(private val cmd: CommandFacade) : SubCommand {
    override val name        = "crash"
    override val usage       = "[percent]"
    override val description = "Fire a server-wide market crash"
    override val permission  = "liveeconomy.admin"
    override val playerOnly  = false

    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val pct = -(args.getOrNull(0)?.toDoubleOrNull()?.let { Math.abs(it) } ?: 15.0)
        applyToAll(cmd, pct, "§c§lMarket crash!")
        sender.sendMessage("${ChatUtil.prefix()}§c§lMarket crash fired (${pct}% on all items).")
        return true
    }
}

private fun applyToAll(cmd: CommandFacade, pct: Double, message: String) {
    cmd.economy.query.getAllItems().keys.forEach { item ->
        cmd.economy.price.applyShock(item, pct, message)
    }
}
