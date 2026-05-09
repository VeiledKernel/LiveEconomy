package dev.liveeconomy.view.message

import dev.liveeconomy.api.economy.PriceService
import dev.liveeconomy.api.player.PortfolioService
import dev.liveeconomy.util.MoneyFormat
import org.bukkit.command.CommandSender
import java.util.UUID

/**
 * Formats and sends the /short list response.
 *
 * Extracted from [dev.liveeconomy.command.trading.ShortListCommand] — that
 * class had 7 sendMessage calls and called portfolio.getShortPositions directly.
 */
class ShortListMessage(
    private val portfolio: PortfolioService,
    private val price:     PriceService
) {
    fun send(sender: CommandSender, playerUuid: UUID) {
        val shorts = portfolio.getShortPositions(playerUuid)

        if (shorts.isEmpty()) {
            sender.sendMessage("§c[Market] §7You have no open short positions.")
            return
        }

        sender.sendMessage("§8§m                              ")
        sender.sendMessage("§c§l  Open Shorts §7(${shorts.size})")
        sender.sendMessage("§8§m                              ")
        shorts.forEach { (item, pos) ->
            val current    = price.getPrice(item) ?: pos.entryPrice
            val unrealised = pos.unrealisedPnl(current)
            val pnlColor   = if (unrealised >= 0) "§a" else "§c"
            sender.sendMessage("  §f${item.displayName()} §7×${pos.quantity}")
            sender.sendMessage(
                "    §7Entry §6${MoneyFormat.full(pos.entryPrice)}" +
                " §7Current §6${MoneyFormat.full(current)}" +
                " §7P&L $pnlColor${MoneyFormat.fullWithSign(unrealised)}"
            )
        }
        sender.sendMessage("§8§m                              ")
    }
}
