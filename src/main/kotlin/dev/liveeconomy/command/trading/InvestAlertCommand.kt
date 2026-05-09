package dev.liveeconomy.command.trading

import dev.liveeconomy.command.CommandFacade
import dev.liveeconomy.command.framework.SubCommand
import dev.liveeconomy.command.framework.requirePlayer
import dev.liveeconomy.command.framework.CompletionEngine
import dev.liveeconomy.data.model.Direction
import dev.liveeconomy.util.ChatUtil
import dev.liveeconomy.util.MoneyFormat
import org.bukkit.command.CommandSender

/**
 * /invest alert <item> <price> <above|below>
 * Creates a price alert for the specified item.
 */
class InvestAlertCommand(private val cmd: CommandFacade) : SubCommand {
    override val name        = "alert"
    override val usage       = "<item> <price> <above|below>"
    override val description = "Set a price alert"
    override val permission  = "liveeconomy.invest"

    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val player = requirePlayer(sender) ?: return true
        if (args.size < 3) return false

        val itemId    = args[0]
        val price     = args[1].toDoubleOrNull() ?: run {
            sender.sendMessage("${ChatUtil.prefix()}§cInvalid price: §f${args[1]}")
            return true
        }
        val direction = when (args[2].lowercase()) {
            "above" -> Direction.ABOVE
            "below" -> Direction.BELOW
            else    -> { sender.sendMessage("${ChatUtil.prefix()}§cMust be §fabove §cor §fbelow§c."); return true }
        }

        // Resolve item key from id
        val item = cmd.economy.query.findItemById(itemId) ?: run {
            sender.sendMessage("${ChatUtil.prefix()}§cItem §f$itemId §cnot found in the market.")
            return true
        }

        when (val result = cmd.alerts.addAlert(player, item.itemKey, price, direction)) {
            is dev.liveeconomy.core.alert.AlertResult.Added -> {
                val dir = if (direction == Direction.ABOVE) "§aabove" else "§cbelow"
                sender.sendMessage("${ChatUtil.prefix()}§7Alert set: §f${item.itemKey.displayName()} §7$dir §6${MoneyFormat.full(price)}§7.")
            }
            is dev.liveeconomy.core.alert.AlertResult.LimitReached ->
                sender.sendMessage("${ChatUtil.prefix()}§cAlert limit reached (§e${result.limit}§c). Prestige or VIP for more.")
            is dev.liveeconomy.core.alert.AlertResult.AlreadyExists ->
                sender.sendMessage("${ChatUtil.prefix()}§cYou already have an alert for §f${item.itemKey.displayName()}§c.")
        }
        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> = when (args.size) {
        1    -> CompletionEngine.marketItems(cmd.economy.query, args[0])
        3    -> CompletionEngine.directions(args[2])
        else -> emptyList()
    }
}
