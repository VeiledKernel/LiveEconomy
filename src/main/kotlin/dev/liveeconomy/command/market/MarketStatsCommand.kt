package dev.liveeconomy.command.market

import dev.liveeconomy.command.CommandFacade
import dev.liveeconomy.command.framework.CompletionEngine
import dev.liveeconomy.command.framework.SubCommand
import dev.liveeconomy.command.framework.requirePlayer
import dev.liveeconomy.util.ChatUtil
import dev.liveeconomy.view.message.MarketStatsMessage
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

/**
 * /market stats [player]
 *
 * Thin adapter — resolves target player, checks permission, delegates
 * all formatting to [MarketStatsMessage].
 */
class MarketStatsCommand(private val cmd: CommandFacade) : SubCommand {
    override val name        = "stats"
    override val usage       = "[player]"
    override val description = "View trader statistics"
    override val permission  = null

    private val message = MarketStatsMessage(cmd.economy.portfolio)

    // Permission to view OTHER players' stats — own stats requires no permission
    private val adminPermission = "liveeconomy.admin"

    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val target = if (args.isNotEmpty()) {
            if (!sender.hasPermission(adminPermission)) {
                sender.sendMessage("${ChatUtil.prefix()}§cYou can only view your own stats.")
                return true
            }
            Bukkit.getPlayer(args[0]) ?: run {
                sender.sendMessage("${ChatUtil.prefix()}§cPlayer §f${args[0]} §cnot found online.")
                return true
            }
        } else {
            requirePlayer(sender) ?: return true
        }

        message.send(sender, target)
        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> =
        if (args.size == 1) CompletionEngine.onlinePlayers(sender, args[0], "liveeconomy.admin")
        else emptyList()
}
