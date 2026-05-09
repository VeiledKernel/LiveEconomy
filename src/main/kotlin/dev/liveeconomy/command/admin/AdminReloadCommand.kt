package dev.liveeconomy.command.admin

import dev.liveeconomy.command.CommandFacade
import dev.liveeconomy.command.framework.SubCommand
import dev.liveeconomy.util.ChatUtil
import org.bukkit.command.CommandSender

class AdminReloadCommand(private val cmd: CommandFacade) : SubCommand {
    override val name        = "reload"
    override val usage       = ""
    override val description = "Reload categories and rebuild market cache"
    override val permission  = "liveeconomy.admin"
    override val playerOnly  = false

    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val result = cmd.reload.reload()
        sender.sendMessage(
            "${ChatUtil.prefix()}§aReloaded §f${result.categoryCount} categories, " +
            "${result.itemCount} items §ain §f${result.elapsedMs}ms§a." +
            if (result.skippedCount > 0) " §e(${result.skippedCount} skipped)" else ""
        )
        return true
    }
}
