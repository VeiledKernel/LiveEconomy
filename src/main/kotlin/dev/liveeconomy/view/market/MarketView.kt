package dev.liveeconomy.view.market

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.data.model.MarketItem

/**
 * Pre-computed view data for [dev.liveeconomy.gui.market.MarketGUI].
 *
 * Owns all derived state that would otherwise leak into the GUI:
 *  - current price per item
 *  - price change percent per item
 *  - isPriceUp flag (for color selection)
 *  - market index + trend label
 *  - items grouped by category, sorted
 */
data class MarketView(
    val categoryItems: Map<String, List<ItemEntry>>,
    val categories:    List<String>,
    val index:         Double,
    val indexTrend:    IndexTrend,
    val alertCount:        Int,
    val balance:           Double,
    val balanceFormatted:  String   // "$1,234.56" — GUI never calls MoneyFormat
) {
    data class ItemEntry(
        val item:         MarketItem,
        val currentPrice: Double,
        val changePct:    Double,
        val isPriceUp:    Boolean
    )

    enum class IndexTrend { BULL, NEUTRAL, BEAR }

    /** Trend based on index value — 1100+ bull, 900–1100 neutral, <900 bear. */
    companion object {
        fun trendFor(index: Double): IndexTrend = when {
            index >= 1100 -> IndexTrend.BULL
            index >= 900  -> IndexTrend.NEUTRAL
            else          -> IndexTrend.BEAR
        }
    }
}
