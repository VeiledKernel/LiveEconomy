package dev.liveeconomy.command.trading

import dev.liveeconomy.command.CommandFacade
import dev.liveeconomy.command.framework.SubCommand
import dev.liveeconomy.command.framework.requirePlayer
import dev.liveeconomy.view.message.ShortListMessage
import org.bukkit.command.CommandSender

/**
 * /short list
 *
 * Thin adapter — resolves player, delegates all formatting to [ShortListMessage].
 */
class ShortListCommand(private val cmd: CommandFacade) : SubCommand {
    override val name        = "list"
    override val usage       = ""
    override val description = "List your open short positions"
    override val permission  = "liveeconomy.vip.short"

    private val message = ShortListMessage(cmd.economy.portfolio, cmd.economy.price)

    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val player = requirePlayer(sender) ?: return true
        message.send(sender, player.uniqueId)
        return true
    }
}
