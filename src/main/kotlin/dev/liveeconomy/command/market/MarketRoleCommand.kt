package dev.liveeconomy.command.market

import dev.liveeconomy.command.CommandFacade
import dev.liveeconomy.command.framework.SubCommand
import dev.liveeconomy.command.framework.requirePlayer
import org.bukkit.command.CommandSender

class MarketRoleCommand(private val cmd: CommandFacade) : SubCommand {
    override val name        = "role"
    override val usage       = ""
    override val description = "Select your trader role"
    override val permission  = null

    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val player = requirePlayer(sender) ?: return true
        cmd.gui.role().open(player)
        return true
    }
}
