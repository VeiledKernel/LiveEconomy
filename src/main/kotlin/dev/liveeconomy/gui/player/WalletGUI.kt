package dev.liveeconomy.gui.player

import dev.liveeconomy.gui.framework.LiveMenu
import dev.liveeconomy.gui.framework.MenuManager
import dev.liveeconomy.gui.shared.ItemBuilder.itemStack
import dev.liveeconomy.gui.shared.Skulls
import dev.liveeconomy.gui.shared.Theme
import dev.liveeconomy.gui.shared.Theme.BOLD
import dev.liveeconomy.gui.shared.Theme.GRAY
import dev.liveeconomy.gui.shared.Theme.YELLOW
import dev.liveeconomy.view.mapper.ViewMapper
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

/**
 * Wallet/profile hub — pure rendering.
 *
 * Zero conditionals. Zero formatting. Zero color decisions.
 * All pre-computed in [dev.liveeconomy.view.wallet.WalletViewBuilder]
 * and accessed via [ViewMapper].
 */
class WalletGUI(
    private val viewMapper:      ViewMapper,
    private val menuManager:     MenuManager,
    private val symbol:          String,
    private val openPortfolio:   (Player) -> Unit,
    private val openOrderBook:   (Player) -> Unit,
    private val openAlerts:      (Player) -> Unit,
    private val openLeaderboard: (Player) -> Unit
) {
    fun open(player: Player) {
        val v    = viewMapper.wallet(player)
        val menu = LiveMenu(
            title = "${Theme.DARK_GRAY}» ${YELLOW}$BOLD${player.name}${Theme.DARK_GRAY}'s Wallet",
            rows  = 6
        )
        for (i in 0 until 54) menu.setItem(i, border())

        // ── Player head ───────────────────────────────────────────
        menu.setItem(13, itemStack(Material.PLAYER_HEAD) {
            name("${YELLOW}$BOLD${player.name}")
            lore(Theme.SEP, "${GRAY}Role      ${Theme.WHITE}${v.role.displayName}",
                "${GRAY}Prestige  ${v.prestigeStars}", Theme.SEP, "${GRAY}${v.role.description}")
            meta { (it as? org.bukkit.inventory.meta.SkullMeta)?.owningPlayer = player }
        })

        // ── Stat items — all strings pre-formatted ────────────────
        menu.setItem(10, Skulls.of(Skulls.GOLD_COIN) {
            name("${Theme.GOLD}${BOLD}Balance")
            lore(Theme.SEP, "${Theme.GOLD}$BOLD${v.balanceFormatted}", Theme.SEP)
        })

        menu.setItem(11, Skulls.of(if (v.isPnlPositive) Skulls.CHART_UP else Skulls.CHART_DOWN) {
            name("${Theme.WHITE}${BOLD}Lifetime P&L")
            lore(Theme.SEP, "${v.pnlColor}$BOLD${v.pnlFormatted}", Theme.SEP)
        })

        menu.setItem(12, Skulls.of(Skulls.CHART_UP) {
            name("${Theme.WHITE}${BOLD}Win Rate")
            lore(Theme.SEP, "${v.winRateColor}$BOLD${v.winRateFormatted}", Theme.SEP,
                "${GRAY}Buys: ${Theme.WHITE}${v.stats.totalBuys}  ${GRAY}Sells: ${Theme.WHITE}${v.stats.totalSells}")
        })

        menu.setItem(14, itemStack(Material.HOPPER) {
            name("${Theme.WHITE}${BOLD}Volume Traded")
            lore(Theme.SEP, "${Theme.WHITE}$BOLD${v.volumeFormatted}", Theme.SEP,
                "${GRAY}Total ${Theme.WHITE}${v.stats.totalTrades} ${GRAY}trades"); hideAll()
        })

        menu.setItem(15, itemStack(Material.NETHER_STAR) {
            name("${Theme.GOLD}${BOLD}Prestige  ${v.prestigeStars}")
            lore(Theme.SEP, "${GRAY}Level  ${Theme.WHITE}${v.prestigeLevelLabel}",
                v.prestigeNote, Theme.SEP)
            if (v.canPrestige) glow(); hideAll()
        })

        menu.setItem(16, itemStack(v.role.iconMaterial) {
            name("${Theme.WHITE}${BOLD}Role: ${Theme.AQUA}${v.role.displayName}")
            lore(Theme.SEP, "${GRAY}${v.role.description}", Theme.SEP,
                "${GRAY}Change via ${YELLOW}/market role"); hideAll()
        })

        // ── Separator — material name from view, no logic here ────
        val sepMat = Material.matchMaterial(v.sepMaterial) ?: Material.GRAY_STAINED_GLASS_PANE
        for (slot in 19..25) menu.setItem(slot, itemStack(sepMat) { name("${Theme.RESET}") })

        // ── Nav buttons ───────────────────────────────────────────
        menu.setSlot(28, itemStack(Material.EMERALD) {
            name("${Theme.GREEN}${BOLD}⚑  Market")
            lore("${GRAY}Browse and trade items", "${YELLOW}Click to open")
        }) { p -> org.bukkit.Sound.UI_BUTTON_CLICK.let { menuManager.kickBack(p) } }

        menu.setSlot(29, itemStack(Material.CHEST) {
            name("${Theme.PURPLE}${BOLD}☰  Portfolio")
            lore("${GRAY}Holdings: ${Theme.WHITE}${v.holdingsCount}  ${GRAY}Shorts: ${Theme.WHITE}${v.shortsCount}",
                "${YELLOW}Click to open")
        }) { p -> openPortfolio(p) }

        menu.setSlot(30, itemStack(Material.BOOK) {
            name("${Theme.AQUA}${BOLD}≡  Orders")
            lore("${GRAY}Active: ${Theme.WHITE}${v.ordersCount}", "${YELLOW}Click to open")
        }) { p -> openOrderBook(p) }

        menu.setSlot(31, Skulls.of(Skulls.ALERT_BELL) {
            name("${YELLOW}${BOLD}🔔  Alerts")
            lore("${GRAY}Active  ${YELLOW}${v.alertsCount} ${GRAY}/ ${Theme.WHITE}${v.alertLimit}",
                "${YELLOW}Click to manage")
        }) { p -> openAlerts(p) }

        menu.setSlot(32, Skulls.of(Skulls.TROPHY) {
            name("${Theme.GOLD}${BOLD}⊞  Leaderboard")
            lore("${GRAY}Top traders by P&L", "${YELLOW}Click to open")
        }) { p -> openLeaderboard(p) }

        menu.setSlot(49, Skulls.of(Skulls.BACK) {
            name("${GRAY}${BOLD}◀  Back")
        }) { p -> menuManager.kickBack(p) }

        menuManager.openAndStack(player, menu)
    }

    private fun border() = itemStack(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }
}
