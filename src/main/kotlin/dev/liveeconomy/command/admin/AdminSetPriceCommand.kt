package dev.liveeconomy.command.admin

import dev.liveeconomy.command.framework.CompletionEngine
import dev.liveeconomy.command.CommandFacade
import dev.liveeconomy.command.framework.SubCommand
import dev.liveeconomy.util.ChatUtil
import dev.liveeconomy.util.MoneyFormat
import org.bukkit.command.CommandSender

/** /leconomy setprice <item> <price> */
class AdminSetPriceCommand(private val cmd: CommandFacade) : SubCommand {
    override val name        = "setprice"
    override val usage       = "<item> <price>"
    override val description = "Override the price of a market item"
    override val permission  = "liveeconomy.admin"
    override val playerOnly  = false

    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size < 2) return false

        val item = cmd.economy.query.findItemById(args[0]) ?: run {
            sender.sendMessage("${ChatUtil.prefix()}§cItem §f${args[0]} §cnot found.")
            return true
        }
        val price = args[1].toDoubleOrNull()?.takeIf { it > 0 } ?: run {
            sender.sendMessage("${ChatUtil.prefix()}§cPrice must be a positive number.")
            return true
        }

        cmd.economy.price.setPrice(item.itemKey, price)
        sender.sendMessage("${ChatUtil.prefix()}§7Price of §f${item.itemKey.displayName()} §7set to §6${MoneyFormat.full(price)}§7.")
        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> =
        if (args.size == 1) CompletionEngine.marketItems(cmd.economy.query, args[0])
        else emptyList()
}
