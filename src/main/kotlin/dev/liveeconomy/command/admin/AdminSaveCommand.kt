package dev.liveeconomy.command.admin

import dev.liveeconomy.command.CommandFacade
import dev.liveeconomy.command.framework.SubCommand
import dev.liveeconomy.util.ChatUtil
import org.bukkit.command.CommandSender

/**
 * /leconomy save — forces an immediate price flush via [PriceService].
 *
 * No storage access — delegates to [PriceService.flushAll] which
 * is the correct service boundary for a save operation.
 */
class AdminSaveCommand(private val cmd: CommandFacade) : SubCommand {
    override val name        = "save"
    override val usage       = ""
    override val description = "Force immediate data save"
    override val permission  = "liveeconomy.admin"
    override val playerOnly  = false

    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val start = System.currentTimeMillis()
        cmd.economy.price.flushAll()
        sender.sendMessage("${ChatUtil.prefix()}§aData saved in §f${System.currentTimeMillis() - start}ms§a.")
        return true
    }
}
