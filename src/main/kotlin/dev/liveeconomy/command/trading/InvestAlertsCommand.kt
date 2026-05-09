package dev.liveeconomy.command.trading

import dev.liveeconomy.command.CommandFacade
import dev.liveeconomy.command.framework.SubCommand
import dev.liveeconomy.command.framework.requirePlayer
import org.bukkit.command.CommandSender

/** /invest alerts — opens PriceAlertGUI */
class InvestAlertsCommand(private val cmd: CommandFacade) : SubCommand {
    override val name        = "alerts"
    override val usage       = ""
    override val description = "Manage your price alerts"
    override val permission  = "liveeconomy.invest"

    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val player = requirePlayer(sender) ?: return true
        cmd.gui.priceAlerts().open(player)
        return true
    }
}
