package dev.liveeconomy.view.wallet

import dev.liveeconomy.api.economy.MarketQueryService
import dev.liveeconomy.api.player.PortfolioService
import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.core.alert.AlertService
import dev.liveeconomy.core.player.PrestigeService
import dev.liveeconomy.core.player.RoleService
import dev.liveeconomy.data.config.PrestigeConfig
import dev.liveeconomy.gui.shared.Theme
import dev.liveeconomy.util.MoneyFormat
import org.bukkit.entity.Player

/**
 * Builds a fully pre-rendered [WalletView].
 *
 * Owns ALL decisions about:
 *  - color thresholds (pnl sign → green/red, win rate → green/yellow/red)
 *  - number formatting (balance, volume, pnl, prestige gap)
 *  - derived labels (prestige stars, level label, note text)
 *  - separator material name
 *
 * GUI receives the view and renders it — zero conditional logic.
 */
class WalletViewBuilder(
    private val wallet:      WalletService,
    private val portfolio:   PortfolioService,
    private val roles:       RoleService,
    private val prestige:    PrestigeService,
    private val alertSvc:    AlertService,
    private val query:       MarketQueryService,
    private val prestigeCfg: PrestigeConfig,
    private val symbol:      String
) {
    fun build(player: Player): WalletView {
        val balance       = wallet.getBalance(player)
        val pnl           = portfolio.getTotalPnl(player.uniqueId).toDouble()
        val stats         = portfolio.getStats(player.uniqueId)
        val role          = roles.getRole(player.uniqueId)
        val prestigeLevel = prestige.getLevel(player.uniqueId)
        val maxPres       = prestigeCfg.maxLevel
        val presReq       = prestigeCfg.requiredPnl
        val holdingsCount = portfolio.getHoldings(player.uniqueId).size
        val shortsCount   = portfolio.getShortPositions(player.uniqueId).size
        val ordersCount   = query.getPlayerOrders(player.uniqueId).size
        val alertsCount   = alertSvc.getAlerts(player.uniqueId).size
        val alertLimit    = alertSvc.getAlertLimit(player)

        // ── Derived booleans ──────────────────────────────────────
        val isPnlPositive = pnl >= 0
        val canPrestige   = pnl >= presReq && prestigeLevel < maxPres
        val pnlToPrestige = (presReq - pnl).coerceAtLeast(0.0)
        val winRatePct    = stats.winRate * 100.0

        // ── Color decisions (thresholds live here, not in GUI) ────
        val pnlColor     = if (isPnlPositive) Theme.GREEN else Theme.RED
        val winRateColor = when {
            winRatePct >= 60 -> Theme.GREEN
            winRatePct >= 40 -> Theme.YELLOW
            else             -> Theme.RED
        }
        val sepMaterial  = if (isPnlPositive) "LIME_STAINED_GLASS_PANE" else "RED_STAINED_GLASS_PANE"

        // ── String formatting (MoneyFormat called here, not in GUI) ──
        val balanceFormatted       = "$symbol${MoneyFormat.full(balance)}"
        val pnlFormatted           = if (isPnlPositive) "+$symbol${MoneyFormat.full(pnl)}"
                                     else               "$symbol${MoneyFormat.full(pnl)}"
        val volumeFormatted        = "$symbol${MoneyFormat.compact(stats.totalVolume)}"
        val pnlToPrestigeFormatted = if (pnlToPrestige > 0) "$symbol${MoneyFormat.full(pnlToPrestige)}" else ""
        val winRateFormatted       = "${String.format("%.1f", winRatePct)}%"
        val prestigeLevelLabel     = "$prestigeLevel / $maxPres"

        // ── Star string ───────────────────────────────────────────
        val prestigeStars = if (maxPres > 0)
            "§6★".repeat(prestigeLevel) + "§8☆".repeat((maxPres - prestigeLevel).coerceAtLeast(0))
        else "§7None"

        // ── Prestige note (decision made here) ───────────────────
        val prestigeNote = when {
            canPrestige            -> "${Theme.GREEN}✔ Ready to prestige!"
            prestigeLevel >= maxPres -> "${Theme.YELLOW}Max prestige reached"
            else                   -> "${Theme.GRAY}Need ${Theme.GOLD}$pnlToPrestigeFormatted more"
        }

        return WalletView(
            role                   = role,
            stats                  = stats,
            holdingsCount          = holdingsCount,
            shortsCount            = shortsCount,
            ordersCount            = ordersCount,
            alertsCount            = alertsCount,
            alertLimit             = alertLimit,
            canPrestige            = canPrestige,
            isPnlPositive          = isPnlPositive,
            balanceFormatted       = balanceFormatted,
            pnlFormatted           = pnlFormatted,
            volumeFormatted        = volumeFormatted,
            pnlToPrestigeFormatted = pnlToPrestigeFormatted,
            prestigeStars          = prestigeStars,
            winRateFormatted       = winRateFormatted,
            prestigeLevelLabel     = prestigeLevelLabel,
            pnlColor               = pnlColor,
            winRateColor           = winRateColor,
            sepMaterial            = sepMaterial,
            prestigeNote           = prestigeNote
        )
    }
}
