package dev.liveeconomy.gui.viewmodel

import dev.liveeconomy.api.economy.MarketQueryService
import dev.liveeconomy.api.player.PortfolioService
import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.core.alert.AlertService
import dev.liveeconomy.core.player.PrestigeService
import dev.liveeconomy.core.player.RoleService
import dev.liveeconomy.data.config.PrestigeConfig
import dev.liveeconomy.util.MoneyFormat
import org.bukkit.entity.Player

/**
 * Builds [WalletView] from services.
 *
 * Owns all derived-state computation that was previously scattered in [WalletGUI]:
 *  - `pnl >= 0` → isPnlPositive
 *  - `pnl >= presReq && level < max` → canPrestige
 *  - `portfolio.getHoldings().size` → holdingsCount
 *  - `portfolio.getShortPositions().size` → shortsCount
 *  - prestige stars string
 *  - pnlFormatted string
 *
 * GUI receives a [WalletView] and only renders it — zero conditional logic.
 */
class WalletViewBuilder(
    private val wallet:      WalletService,
    private val portfolio:   PortfolioService,
    private val roles:       RoleService,
    private val prestige:    PrestigeService,
    private val alertSvc:    AlertService,
    private val query:       MarketQueryService,
    private val prestigeCfg: PrestigeConfig
) {
    fun build(player: Player): WalletView {
        val balance        = wallet.getBalance(player)
        val pnl            = portfolio.getTotalPnl(player.uniqueId).toDouble()
        val stats          = portfolio.getStats(player.uniqueId)
        val role           = roles.getRole(player.uniqueId)
        val prestigeLevel  = prestige.getLevel(player.uniqueId)
        val maxPres        = prestigeCfg.maxLevel
        val presReq        = prestigeCfg.requiredPnl
        val holdingsCount  = portfolio.getHoldings(player.uniqueId).size
        val shortsCount    = portfolio.getShortPositions(player.uniqueId).size
        val ordersCount    = query.getPlayerOrders(player.uniqueId).size
        val alertsCount    = alertSvc.getAlerts(player.uniqueId).size
        val alertLimit     = alertSvc.getAlertLimit(player)

        val isPnlPositive = pnl >= 0
        val canPrestige   = pnl >= presReq && prestigeLevel < maxPres
        val pnlFormatted  = if (isPnlPositive) "+${MoneyFormat.full(pnl)}" else MoneyFormat.full(pnl)
        val pnlToPrestige = if (canPrestige) 0.0 else (presReq - pnl).coerceAtLeast(0.0)

        val starsFull  = "§6★".repeat(prestigeLevel)
        val starsEmpty = "§8☆".repeat((maxPres - prestigeLevel).coerceAtLeast(0))
        val prestigeStars = if (maxPres > 0) "$starsFull$starsEmpty" else "§7None"

        return WalletView(
            balance        = balance,
            pnl            = pnl,
            isPnlPositive  = isPnlPositive,
            pnlFormatted   = pnlFormatted,
            canPrestige    = canPrestige,
            prestigeLevel  = prestigeLevel,
            maxPrestige    = maxPres,
            prestigeStars  = prestigeStars,
            pnlToPrestige  = pnlToPrestige,
            role           = role,
            stats          = stats,
            holdingsCount  = holdingsCount,
            shortsCount    = shortsCount,
            ordersCount    = ordersCount,
            alertsCount    = alertsCount,
            alertLimit     = alertLimit,
            winRatePct     = stats.winRate * 100.0
        )
    }
}
