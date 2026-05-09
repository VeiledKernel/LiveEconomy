package dev.liveeconomy.command.trading

import dev.liveeconomy.command.CommandFacade
import dev.liveeconomy.command.framework.SubCommand
import dev.liveeconomy.command.framework.requirePlayer
import dev.liveeconomy.core.player.PrestigeResult
import dev.liveeconomy.util.ChatUtil
import org.bukkit.command.CommandSender

/** /invest prestige — attempts a prestige if eligible */
class InvestPrestigeCommand(private val cmd: CommandFacade) : SubCommand {
    override val name        = "prestige"
    override val usage       = ""
    override val description = "Prestige if you meet the P&L requirement"
    override val permission  = "liveeconomy.vip.prestige"

    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val player = requirePlayer(sender) ?: return true

        when (val result = cmd.prestige.attempt(player.uniqueId)) {
            is PrestigeResult.Success ->
                sender.sendMessage("${ChatUtil.prefix()}§6✦ §ePrestiged to §6Level ${result.newLevel}§e! " +
                    "§7Your P&L has been reset.")
            is PrestigeResult.NotEligible ->
                sender.sendMessage("${ChatUtil.prefix()}§cNot eligible. Need §6${result.requiredPnlFormatted} §cP&L.")
            is PrestigeResult.MaxLevel ->
                sender.sendMessage("${ChatUtil.prefix()}§eYou have reached the maximum prestige level.")
        }
        return true
    }
}
