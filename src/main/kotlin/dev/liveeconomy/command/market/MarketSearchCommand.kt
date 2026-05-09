package dev.liveeconomy.command.market

import dev.liveeconomy.command.CommandFacade
import dev.liveeconomy.command.framework.SubCommand
import dev.liveeconomy.command.framework.requirePlayer
import org.bukkit.command.CommandSender

class MarketSearchCommand(private val cmd: CommandFacade) : SubCommand {
    override val name        = "search"
    override val usage       = ""
    override val description = "Browse all market items"
    override val permission  = null

    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val player = requirePlayer(sender) ?: return true
        cmd.gui.search().open(player)
        return true
    }
}
