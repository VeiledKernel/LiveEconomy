package dev.liveeconomy.view.wallet

import dev.liveeconomy.data.model.PlayerRole
import dev.liveeconomy.data.model.PlayerStats

/**
 * Pre-computed view data for [dev.liveeconomy.gui.player.WalletGUI].
 *
 * All derived state — including colors and formatted strings — computed
 * once by [WalletViewBuilder]. GUI is pure rendering: no conditionals,
 * no formatting, no color threshold decisions.
 */
data class WalletView(
    // ── Raw values (for nav logic only) ──────────────────────────────────────
    val role:           PlayerRole,
    val stats:          PlayerStats,
    val holdingsCount:  Int,
    val shortsCount:    Int,
    val ordersCount:    Int,
    val alertsCount:    Int,
    val alertLimit:     Int,
    val canPrestige:    Boolean,
    val isPnlPositive:  Boolean,

    // ── Pre-formatted strings — GUI never calls MoneyFormat ───────────────────
    val balanceFormatted:       String,   // "$1,234.56"
    val pnlFormatted:           String,   // "+$1,234.56" or "-$234.56"
    val volumeFormatted:        String,   // "$98.5K"
    val pnlToPrestigeFormatted: String,   // "$500.00 more" or ""
    val prestigeStars:          String,   // "§6★★§8☆☆☆"
    val winRateFormatted:       String,   // "62.5%"
    val prestigeLevelLabel:     String,   // "2 / 5"

    // ── Pre-computed color strings — GUI never calls Theme.GREEN etc ──────────
    val pnlColor:       String,   // Theme.GREEN or Theme.RED
    val winRateColor:   String,   // Theme.GREEN / YELLOW / RED
    val sepMaterial:    String,   // "LIME_STAINED_GLASS_PANE" or "RED_..."
    val prestigeNote:   String    // "§a✔ Ready!" / "§7Max" / "§7Need $X more"
)
