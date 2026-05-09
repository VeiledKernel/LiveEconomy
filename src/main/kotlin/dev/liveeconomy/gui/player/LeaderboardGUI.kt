package dev.liveeconomy.gui.player

import dev.liveeconomy.api.player.PortfolioService
import dev.liveeconomy.core.player.RoleService
import dev.liveeconomy.gui.framework.LiveMenu
import dev.liveeconomy.gui.framework.MenuManager
import dev.liveeconomy.gui.shared.ItemBuilder.itemStack
import dev.liveeconomy.gui.shared.Skulls
import dev.liveeconomy.gui.shared.Theme
import dev.liveeconomy.util.MoneyFormat
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

/** Leaderboard — top traders by P&L among online players. */
class LeaderboardGUI(
    private val portfolio:   PortfolioService,
    private val roleService: RoleService,
    private val menuManager: MenuManager,
    private val symbol:      String
) {
    fun open(player: Player) {
        val ranked = Bukkit.getOnlinePlayers()
            .map { p -> p to portfolio.getTotalPnl(p.uniqueId) }
            .sortedByDescending { it.second }
            .take(18)

        val menu = LiveMenu("§0§l» §6§lTrader Leaderboard", rows = 4)
        for (i in 0 until 36) menu.setItem(i, border())

        ranked.forEachIndexed { index, (p, pnl) ->
            val color    = if (pnl.toDouble() >= 0) "§a" else "§c"
            val stats    = portfolio.getStats(p.uniqueId)
            val prestige = portfolio.getPrestigeLevel(p.uniqueId)
            val role     = roleService.getRole(p.uniqueId)
            menu.setItem(9 + index, itemStack(Material.PLAYER_HEAD) {
                name("§f§l${p.name}")
                lore(
                    "§7P&L:      $color$symbol${MoneyFormat.full(pnl.toDouble())}",
                    "§7Win Rate: §f${String.format("%.1f", stats.winRate * 100)}%",
                    "§7Role:     §f${role.displayName}",
                    if (prestige > 0) "§6★ Prestige $prestige" else ""
                )
                meta { meta ->
                    (meta as? org.bukkit.inventory.meta.SkullMeta)?.owningPlayer = p
                }
            })
        }

        menu.setSlot(31, Skulls.of(Skulls.BACK) { name("${Theme.GRAY}◀ Back") }) {
            menuManager.kickBack(it)
        }
        menuManager.openAndStack(player, menu)
    }

    private fun border() = itemStack(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }
}
