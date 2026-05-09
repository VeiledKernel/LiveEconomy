package dev.liveeconomy.command.trading

import dev.liveeconomy.api.economy.result.ShortResult
import dev.liveeconomy.command.CommandFacade
import dev.liveeconomy.command.framework.SubCommand
import dev.liveeconomy.command.framework.requirePlayer
import dev.liveeconomy.command.framework.CompletionEngine
import dev.liveeconomy.util.ChatUtil
import dev.liveeconomy.util.MoneyFormat
import org.bukkit.command.CommandSender

/** /short close <item> */
class ShortCloseCommand(private val cmd: CommandFacade) : SubCommand {
    override val name        = "close"
    override val usage       = "<item>"
    override val description = "Close a short position"
    override val permission  = "liveeconomy.vip.short"

    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val player = requirePlayer(sender) ?: return true
        if (args.isEmpty()) return false

        val item = cmd.economy.query.findItemById(args[0]) ?: run {
            sender.sendMessage("${ChatUtil.prefix()}§cItem §f${args[0]} §cnot found.")
            return true
        }

        when (val result = cmd.economy.trade.closeShort(player, item.itemKey)) {
            is ShortResult.Closed -> {
                val pnlColor = if (result.pnl >= 0) "§a" else "§c"
                sender.sendMessage("${ChatUtil.prefix()}§7Short closed. P&L: $pnlColor${MoneyFormat.fullWithSign(result.pnl)}")
            }
            is ShortResult.NotFound ->
                sender.sendMessage("${ChatUtil.prefix()}§cNo open short for §f${item.itemKey.displayName()}§c.")
            else ->
                sender.sendMessage("${ChatUtil.prefix()}§cCould not close short position.")
        }
        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size != 1) return emptyList()
        val player = sender as? org.bukkit.entity.Player ?: return emptyList()
        return CompletionEngine.shortPositions(cmd.economy.portfolio, player.uniqueId, args[0])
    }
}
