package dev.liveeconomy.view.message

import dev.liveeconomy.api.player.PortfolioService
import dev.liveeconomy.util.MoneyFormat
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Formats and sends the /market stats response.
 *
 * Extracted from [dev.liveeconomy.command.market.MarketStatsCommand] — that
 * class had 11 sendMessage calls inline, violating the thin-adapter rule.
 *
 * The command resolves the target player and permission, then delegates here.
 */
class MarketStatsMessage(private val portfolio: PortfolioService) {

    fun send(sender: CommandSender, target: Player) {
        val stats    = portfolio.getStats(target.uniqueId)
        val pnl      = portfolio.getTotalPnl(target.uniqueId).toDouble()
        val pnlColor = if (pnl >= 0) "§a" else "§c"

        sender.sendMessage("§8§m                              ")
        sender.sendMessage("§6§l  ${target.name}§7's Stats")
        sender.sendMessage("§8§m                              ")
        sender.sendMessage("  §7P&L         $pnlColor${MoneyFormat.fullWithSign(pnl)}")
        sender.sendMessage("  §7Win rate    §f${String.format("%.1f", stats.winRate * 100)}%")
        sender.sendMessage("  §7Buys        §f${stats.totalBuys}")
        sender.sendMessage("  §7Sells       §f${stats.totalSells}")
        sender.sendMessage("  §7Volume      §f${MoneyFormat.compact(stats.totalVolume)}")
        sender.sendMessage("§8§m                              ")
    }
}
