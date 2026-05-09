package dev.liveeconomy.view.portfolio

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.data.model.PlayerStats
import dev.liveeconomy.data.model.ShortPosition
import dev.liveeconomy.data.model.Transaction

/**
 * Pre-computed view data for [dev.liveeconomy.gui.player.PortfolioGUI].
 */
data class PortfolioView(
    val pnl:           Double,
    val stats:         PlayerStats,
    val holdings:      Map<ItemKey, HoldingEntry>,
    val shorts:        Map<ItemKey, ShortEntry>,
    val recentTxs:     List<Transaction>
) {
    data class HoldingEntry(
        val quantity:     Int,
        val currentPrice: Double
    )

    data class ShortEntry(
        val position:     ShortPosition,
        val currentPrice: Double,
        val unrealisedPnl: Double,
        val isPnlPositive: Boolean
    )
}
