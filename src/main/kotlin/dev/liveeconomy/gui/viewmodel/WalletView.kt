package dev.liveeconomy.gui.viewmodel

import dev.liveeconomy.data.model.PlayerRole
import dev.liveeconomy.data.model.PlayerStats

/**
 * Pre-computed view data for [dev.liveeconomy.gui.player.WalletGUI].
 *
 * All derived state computed once by [WalletViewBuilder] — GUI only renders.
 * No `.size`, `.count`, or conditional logic in the GUI class.
 */
data class WalletView(
    val balance:        Double,
    val pnl:            Double,
    val isPnlPositive:  Boolean,
    val pnlFormatted:   String,       // "+1,234.56" or "-234.56"
    val canPrestige:    Boolean,
    val prestigeLevel:  Int,
    val maxPrestige:    Int,
    val prestigeStars:  String,       // "§6★★§8☆☆☆"
    val pnlToPrestige:  Double,       // how much more P&L needed (0 if eligible)
    val role:           PlayerRole,
    val stats:          PlayerStats,
    val holdingsCount:  Int,
    val shortsCount:    Int,
    val ordersCount:    Int,
    val alertsCount:    Int,
    val alertLimit:     Int,
    val winRatePct:     Double        // 0.0–100.0
)
