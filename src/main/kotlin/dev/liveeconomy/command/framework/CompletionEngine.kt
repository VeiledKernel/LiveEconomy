package dev.liveeconomy.command.framework

import dev.liveeconomy.api.economy.MarketQueryService
import dev.liveeconomy.api.player.PortfolioService
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Reusable tab-completion helpers.
 *
 * Extracted from 4 duplicated patterns across command files:
 *  - market item IDs  (`getAllItems → itemKey.id → filter prefix`)
 *  - online player names (admin-gated)
 *  - short position item IDs (per-player)
 *  - direction keywords (above/below)
 *
 * Rules:
 *  - pure functions, no state
 *  - each helper filters to the current partial arg automatically
 *  - never throws — returns emptyList on bad input
 */
object CompletionEngine {

    /** All market item IDs matching [prefix]. */
    fun marketItems(query: MarketQueryService, prefix: String): List<String> =
        query.getAllItems().values
            .map { it.itemKey.id }
            .filter { it.startsWith(prefix, ignoreCase = true) }
            .sorted()

    /** Online player names matching [prefix] — only if sender has [requiredPermission]. */
    fun onlinePlayers(sender: CommandSender, prefix: String, requiredPermission: String? = null): List<String> {
        if (requiredPermission != null && !sender.hasPermission(requiredPermission)) return emptyList()
        return Bukkit.getOnlinePlayers()
            .map { it.name }
            .filter { it.startsWith(prefix, ignoreCase = true) }
    }

    /** Item IDs of [playerUuid]'s open short positions matching [prefix]. */
    fun shortPositions(portfolio: PortfolioService, playerUuid: UUID, prefix: String): List<String> =
        portfolio.getShortPositions(playerUuid).keys
            .map { it.id }
            .filter { it.startsWith(prefix, ignoreCase = true) }

    /** "above" / "below" direction keywords matching [prefix]. */
    fun directions(prefix: String): List<String> =
        listOf("above", "below").filter { it.startsWith(prefix, ignoreCase = true) }
}
