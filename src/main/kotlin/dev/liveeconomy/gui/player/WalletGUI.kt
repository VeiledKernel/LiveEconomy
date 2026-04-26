package dev.liveeconomy.gui.player

import dev.liveeconomy.api.economy.MarketQueryService
import dev.liveeconomy.api.economy.PriceService
import dev.liveeconomy.api.player.PortfolioService
import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.core.alert.AlertService
import dev.liveeconomy.core.player.PrestigeService
import dev.liveeconomy.core.player.RoleService
import dev.liveeconomy.data.config.PrestigeConfig
import dev.liveeconomy.gui.framework.LiveMenu
import dev.liveeconomy.gui.framework.MenuManager
import dev.liveeconomy.gui.shared.ItemBuilder.itemStack
import dev.liveeconomy.gui.shared.Skulls
import dev.liveeconomy.gui.shared.Theme
import dev.liveeconomy.util.ChatUtil
import dev.liveeconomy.util.MoneyFormat
import dev.liveeconomy.util.SoundUtil
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

/**
 * Wallet/profile hub — balance, stats, role, prestige, navigation.
 * No [plugin.xxx] references.
 */
class WalletGUI(
    private val wallet:        WalletService,
    private val portfolio:     PortfolioService,
    private val price:         PriceService,
    private val query:         MarketQueryService,
    private val roleService:   RoleService,
    private val prestige:      PrestigeService,
    private val alertService:  AlertService,
    private val prestigeCfg:   PrestigeConfig,
    private val menuManager:   MenuManager,
    private val symbol:        String,
    // GUI references for navigation — injected to avoid circular deps
    private val openPortfolio: (Player) -> Unit,
    private val openOrderBook: (Player) -> Unit,
    private val openAlerts:    (Player) -> Unit,
    private val openLeaderboard: (Player) -> Unit
) {
    fun open(player: Player) {
        val balance  = wallet.getBalance(player)
        val role     = roleService.getRole(player.uniqueId)
        val prestigeLevel = prestige.getLevel(player.uniqueId)
        val maxPres  = prestigeCfg.maxLevel
        val stats    = portfolio.getStats(player.uniqueId)
        val pnl      = portfolio.getTotalPnl(player.uniqueId).toDouble()
        val holdings = portfolio.getHoldings(player.uniqueId).size
        val shorts   = portfolio.getShortPositions(player.uniqueId).size
        val alerts   = alertService.getAlerts(player.uniqueId).size
        val alertMax = alertService.getAlertLimit(player)
        val orders   = query.getPlayerOrders(player.uniqueId).size
        val pnlColor = if (pnl >= 0) Theme.GREEN else Theme.RED
        val pnlStr   = if (pnl >= 0) "+${MoneyFormat.full(pnl)}" else MoneyFormat.full(pnl)
        val presReq  = prestigeCfg.requiredPnl
        val canPres  = pnl >= presReq && prestigeLevel < maxPres

        val starsFull  = "§6★".repeat(prestigeLevel)
        val starsEmpty = "§8☆".repeat((maxPres - prestigeLevel).coerceAtLeast(0))
        val prestigeDisplay = if (maxPres > 0) "$starsFull$starsEmpty" else "${Theme.GRAY}None"

        val menu = LiveMenu(
            title = "${Theme.DARK_GRAY}» ${Theme.YELLOW}${Theme.BOLD}${player.name}${Theme.DARK_GRAY}'s Wallet",
            rows  = 6
        )
        for (i in 0 until 54) menu.setItem(i, border())

        // ── Player head ───────────────────────────────────────────
        menu.setItem(13, itemStack(Material.PLAYER_HEAD) {
            name("${Theme.YELLOW}${Theme.BOLD}${player.name}")
            lore(Theme.SEP,
                "${Theme.GRAY}Role      ${Theme.WHITE}${role.displayName}",
                "${Theme.GRAY}Prestige  $prestigeDisplay",
                Theme.SEP, "${Theme.GRAY}${role.description}")
            meta { (it as? org.bukkit.inventory.meta.SkullMeta)?.owningPlayer = player }
        })

        // ── Stat items ────────────────────────────────────────────
        menu.setItem(10, Skulls.of(Skulls.GOLD_COIN) {
            name("${Theme.GOLD}${Theme.BOLD}Balance")
            lore(Theme.SEP, "${Theme.GOLD}${Theme.BOLD}$symbol${MoneyFormat.full(balance)}", Theme.SEP)
        })

        menu.setItem(11, Skulls.of(if (pnl >= 0) Skulls.CHART_UP else Skulls.CHART_DOWN) {
            name("${Theme.WHITE}${Theme.BOLD}Lifetime P&L")
            lore(Theme.SEP, "$pnlColor${Theme.BOLD}$symbol$pnlStr", Theme.SEP)
        })

        val wrColor = when { stats.winRate * 100 >= 60 -> Theme.GREEN; stats.winRate * 100 >= 40 -> Theme.YELLOW; else -> Theme.RED }
        menu.setItem(12, Skulls.of(Skulls.CHART_UP) {
            name("${Theme.WHITE}${Theme.BOLD}Win Rate")
            lore(Theme.SEP, "$wrColor${Theme.BOLD}${String.format("%.1f", stats.winRate * 100)}%", Theme.SEP,
                "${Theme.GRAY}Buys: ${Theme.WHITE}${stats.totalBuys}  ${Theme.GRAY}Sells: ${Theme.WHITE}${stats.totalSells}")
        })

        menu.setItem(14, itemStack(Material.HOPPER) {
            name("${Theme.WHITE}${Theme.BOLD}Volume Traded")
            lore(Theme.SEP, "${Theme.WHITE}${Theme.BOLD}$symbol${MoneyFormat.compact(stats.totalVolume)}", Theme.SEP,
                "${Theme.GRAY}Total ${Theme.WHITE}${stats.totalTrades} ${Theme.GRAY}trades"); hideAll()
        })

        menu.setItem(15, itemStack(Material.NETHER_STAR) {
            name("${Theme.GOLD}${Theme.BOLD}Prestige  $prestigeDisplay")
            lore(Theme.SEP, "${Theme.GRAY}Level  ${Theme.WHITE}$prestigeLevel / $maxPres",
                "${Theme.GRAY}Req    ${Theme.GOLD}$symbol${MoneyFormat.full(presReq)} P&L", Theme.SEP,
                if (canPres) "${Theme.GREEN}✔ Ready to prestige!"
                else if (prestigeLevel >= maxPres) "${Theme.YELLOW}Max prestige reached"
                else "${Theme.GRAY}Need ${Theme.GOLD}$symbol${MoneyFormat.full((presReq - pnl).coerceAtLeast(0.0))} more")
            if (canPres) glow(); hideAll()
        })

        menu.setItem(16, itemStack(role.iconMaterial) {
            name("${Theme.WHITE}${Theme.BOLD}Role: ${Theme.AQUA}${role.displayName}")
            lore(Theme.SEP, "${Theme.GRAY}${role.description}", Theme.SEP,
                "${Theme.GRAY}Change via ${Theme.YELLOW}/market role"); hideAll()
        })

        // ── Separator ─────────────────────────────────────────────
        val sepMat = if (pnl >= 0) Material.LIME_STAINED_GLASS_PANE else Material.RED_STAINED_GLASS_PANE
        for (slot in 19..25) menu.setItem(slot, itemStack(sepMat) { name("${Theme.RESET}") })

        // ── Nav buttons ───────────────────────────────────────────
        menu.setSlot(28, itemStack(Material.EMERALD) {
            name("${Theme.GREEN}${Theme.BOLD}⚑  Market")
            lore("${Theme.GRAY}Browse and trade items", "${Theme.YELLOW}Click to open")
        }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); menuManager.kickBack(p) }

        menu.setSlot(29, itemStack(Material.CHEST) {
            name("${Theme.PURPLE}${Theme.BOLD}☰  Portfolio")
            lore("${Theme.GRAY}Holdings: ${Theme.WHITE}$holdings  ${Theme.GRAY}Shorts: ${Theme.WHITE}$shorts",
                "${Theme.YELLOW}Click to open")
        }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); openPortfolio(p) }

        menu.setSlot(30, itemStack(Material.BOOK) {
            name("${Theme.AQUA}${Theme.BOLD}≡  Orders")
            lore("${Theme.GRAY}Active orders  ${Theme.WHITE}$orders", "${Theme.YELLOW}Click to open")
        }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); openOrderBook(p) }

        menu.setSlot(31, Skulls.of(Skulls.ALERT_BELL) {
            name("${Theme.YELLOW}${Theme.BOLD}🔔  Alerts")
            lore("${Theme.GRAY}Active  ${Theme.YELLOW}$alerts ${Theme.GRAY}/ ${Theme.WHITE}$alertMax",
                "${Theme.YELLOW}Click to manage")
        }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); openAlerts(p) }

        menu.setSlot(32, Skulls.of(Skulls.TROPHY) {
            name("${Theme.GOLD}${Theme.BOLD}⊞  Leaderboard")
            lore("${Theme.GRAY}Top traders by P&L", "${Theme.YELLOW}Click to open")
        }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); openLeaderboard(p) }

        // ── Back ──────────────────────────────────────────────────
        menu.setSlot(49, Skulls.of(Skulls.BACK) {
            name("${Theme.GRAY}${Theme.BOLD}◀  Back")
            lore("${Theme.GRAY}Return to market")
        }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); menuManager.kickBack(p) }

        menuManager.openAndStack(player, menu)
    }

    private fun border() = itemStack(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }
}
