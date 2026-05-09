package dev.liveeconomy.gui.player

import dev.liveeconomy.gui.framework.LiveMenu
import dev.liveeconomy.gui.framework.MenuManager
import dev.liveeconomy.gui.shared.ItemBuilder.itemStack
import dev.liveeconomy.gui.shared.Skulls
import dev.liveeconomy.gui.shared.Theme
import dev.liveeconomy.view.mapper.ViewMapper
import dev.liveeconomy.util.MoneyFormat
import dev.liveeconomy.util.SoundUtil
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

/**
 * Wallet/profile hub — renders a [WalletView], zero derived state.
 *
 * All computation (pnl sign, canPrestige, star strings, counts) lives
 * in [WalletViewBuilder]. This class only maps view model fields to slots.
 */
class WalletGUI(
    private val viewMapper:     ViewMapper,
    private val menuManager:    MenuManager,
    private val symbol:         String,
    private val openPortfolio:  (Player) -> Unit,
    private val openOrderBook:  (Player) -> Unit,
    private val openAlerts:     (Player) -> Unit,
    private val openLeaderboard: (Player) -> Unit
) {
    fun open(player: Player) {
        val v    = viewMapper.wallet(player)
        val menu = LiveMenu(
            title = "${Theme.DARK_GRAY}» ${Theme.YELLOW}${Theme.BOLD}${player.name}${Theme.DARK_GRAY}'s Wallet",
            rows  = 6
        )
        for (i in 0 until 54) menu.setItem(i, border())

        // ── Player head ───────────────────────────────────────────
        menu.setItem(13, itemStack(Material.PLAYER_HEAD) {
            name("${Theme.YELLOW}${Theme.BOLD}${player.name}")
            lore(Theme.SEP,
                "${Theme.GRAY}Role      ${Theme.WHITE}${v.role.displayName}",
                "${Theme.GRAY}Prestige  ${v.prestigeStars}",
                Theme.SEP, "${Theme.GRAY}${v.role.description}")
            meta { (it as? org.bukkit.inventory.meta.SkullMeta)?.owningPlayer = player }
        })

        // ── Stat items ────────────────────────────────────────────
        menu.setItem(10, Skulls.of(Skulls.GOLD_COIN) {
            name("${Theme.GOLD}${Theme.BOLD}Balance")
            lore(Theme.SEP, "${Theme.GOLD}${Theme.BOLD}$symbol${MoneyFormat.full(v.balance)}", Theme.SEP)
        })

        val pnlColor = if (v.isPnlPositive) Theme.GREEN else Theme.RED
        menu.setItem(11, Skulls.of(if (v.isPnlPositive) Skulls.CHART_UP else Skulls.CHART_DOWN) {
            name("${Theme.WHITE}${Theme.BOLD}Lifetime P&L")
            lore(Theme.SEP, "$pnlColor${Theme.BOLD}$symbol${v.pnlFormatted}", Theme.SEP)
        })

        val wrColor = when { v.winRatePct >= 60 -> Theme.GREEN; v.winRatePct >= 40 -> Theme.YELLOW; else -> Theme.RED }
        menu.setItem(12, Skulls.of(Skulls.CHART_UP) {
            name("${Theme.WHITE}${Theme.BOLD}Win Rate")
            lore(Theme.SEP, "$wrColor${Theme.BOLD}${String.format("%.1f", v.winRatePct)}%", Theme.SEP,
                "${Theme.GRAY}Buys: ${Theme.WHITE}${v.stats.totalBuys}  ${Theme.GRAY}Sells: ${Theme.WHITE}${v.stats.totalSells}")
        })

        menu.setItem(14, itemStack(Material.HOPPER) {
            name("${Theme.WHITE}${Theme.BOLD}Volume Traded")
            lore(Theme.SEP, "${Theme.WHITE}${Theme.BOLD}$symbol${MoneyFormat.compact(v.stats.totalVolume)}", Theme.SEP,
                "${Theme.GRAY}Total ${Theme.WHITE}${v.stats.totalTrades} ${Theme.GRAY}trades"); hideAll()
        })

        menu.setItem(15, itemStack(Material.NETHER_STAR) {
            name("${Theme.GOLD}${Theme.BOLD}Prestige  ${v.prestigeStars}")
            lore(Theme.SEP,
                "${Theme.GRAY}Level  ${Theme.WHITE}${v.prestigeLevel} / ${v.maxPrestige}",
                "${Theme.GRAY}Req    ${Theme.GOLD}$symbol${MoneyFormat.full(v.pnl + v.pnlToPrestige)} P&L",
                Theme.SEP,
                if (v.canPrestige) "${Theme.GREEN}✔ Ready to prestige!"
                else if (v.prestigeLevel >= v.maxPrestige) "${Theme.YELLOW}Max prestige reached"
                else "${Theme.GRAY}Need ${Theme.GOLD}$symbol${MoneyFormat.full(v.pnlToPrestige)} more")
            if (v.canPrestige) glow(); hideAll()
        })

        menu.setItem(16, itemStack(v.role.iconMaterial) {
            name("${Theme.WHITE}${Theme.BOLD}Role: ${Theme.AQUA}${v.role.displayName}")
            lore(Theme.SEP, "${Theme.GRAY}${v.role.description}", Theme.SEP,
                "${Theme.GRAY}Change via ${Theme.YELLOW}/market role"); hideAll()
        })

        // ── Separator ─────────────────────────────────────────────
        val sepMat = if (v.isPnlPositive) Material.LIME_STAINED_GLASS_PANE else Material.RED_STAINED_GLASS_PANE
        for (slot in 19..25) menu.setItem(slot, itemStack(sepMat) { name("${Theme.RESET}") })

        // ── Nav buttons ───────────────────────────────────────────
        menu.setSlot(28, itemStack(Material.EMERALD) {
            name("${Theme.GREEN}${Theme.BOLD}⚑  Market")
            lore("${Theme.GRAY}Browse and trade items", "${Theme.YELLOW}Click to open")
        }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); menuManager.kickBack(p) }

        menu.setSlot(29, itemStack(Material.CHEST) {
            name("${Theme.PURPLE}${Theme.BOLD}☰  Portfolio")
            lore("${Theme.GRAY}Holdings: ${Theme.WHITE}${v.holdingsCount}  ${Theme.GRAY}Shorts: ${Theme.WHITE}${v.shortsCount}",
                "${Theme.YELLOW}Click to open")
        }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); openPortfolio(p) }

        menu.setSlot(30, itemStack(Material.BOOK) {
            name("${Theme.AQUA}${Theme.BOLD}≡  Orders")
            lore("${Theme.GRAY}Active orders  ${Theme.WHITE}${v.ordersCount}", "${Theme.YELLOW}Click to open")
        }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); openOrderBook(p) }

        menu.setSlot(31, Skulls.of(Skulls.ALERT_BELL) {
            name("${Theme.YELLOW}${Theme.BOLD}🔔  Alerts")
            lore("${Theme.GRAY}Active  ${Theme.YELLOW}${v.alertsCount} ${Theme.GRAY}/ ${Theme.WHITE}${v.alertLimit}",
                "${Theme.YELLOW}Click to manage")
        }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); openAlerts(p) }

        menu.setSlot(32, Skulls.of(Skulls.TROPHY) {
            name("${Theme.GOLD}${Theme.BOLD}⊞  Leaderboard")
            lore("${Theme.GRAY}Top traders by P&L", "${Theme.YELLOW}Click to open")
        }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); openLeaderboard(p) }

        menu.setSlot(49, Skulls.of(Skulls.BACK) {
            name("${Theme.GRAY}${Theme.BOLD}◀  Back")
        }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); menuManager.kickBack(p) }

        menuManager.openAndStack(player, menu)
    }

    private fun border() = itemStack(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }
}
