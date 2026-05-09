package dev.liveeconomy.command.trading

import dev.liveeconomy.api.economy.result.ShortResult
import dev.liveeconomy.command.CommandFacade
import dev.liveeconomy.command.framework.SubCommand
import dev.liveeconomy.command.framework.requirePlayer
import dev.liveeconomy.command.framework.CompletionEngine
import dev.liveeconomy.util.ChatUtil
import dev.liveeconomy.util.MoneyFormat
import org.bukkit.command.CommandSender

/** /short open <item> <quantity> */
class ShortOpenCommand(private val cmd: CommandFacade) : SubCommand {
    override val name        = "open"
    override val usage       = "<item> <quantity>"
    override val description = "Open a short position"
    override val permission  = "liveeconomy.vip.short"

    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val player = requirePlayer(sender) ?: return true
        if (args.size < 2) return false

        val item = cmd.economy.query.findItemById(args[0]) ?: run {
            sender.sendMessage("${ChatUtil.prefix()}§cItem §f${args[0]} §cnot found.")
            return true
        }
        val qty = args[1].toIntOrNull()?.takeIf { it > 0 } ?: run {
            sender.sendMessage("${ChatUtil.prefix()}§cQuantity must be a positive number.")
            return true
        }

        when (val result = cmd.economy.trade.openShort(player, item.itemKey, qty)) {
            is ShortResult.Opened ->
                sender.sendMessage("${ChatUtil.prefix()}§cShort opened: §f×$qty ${item.itemKey.displayName()} " +
                    "§7@ §6${MoneyFormat.full(result.entryPrice)}§7. " +
                    "Collateral locked: §c${MoneyFormat.full(result.collateral)}")
            is ShortResult.InsufficientFunds ->
                sender.sendMessage("${ChatUtil.prefix()}§cInsufficient funds for collateral.")
            is ShortResult.ShortingDisabled ->
                sender.sendMessage("${ChatUtil.prefix()}§cShort selling is disabled on this server.")
            is ShortResult.AlreadyShorted ->
                sender.sendMessage("${ChatUtil.prefix()}§cYou already have a short on §f${item.itemKey.displayName()}§c.")
            else ->
                sender.sendMessage("${ChatUtil.prefix()}§cCould not open short position.")
        }
        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> =
        if (args.size == 1) CompletionEngine.marketItems(cmd.economy.query, args[0])
        else emptyList()
}
