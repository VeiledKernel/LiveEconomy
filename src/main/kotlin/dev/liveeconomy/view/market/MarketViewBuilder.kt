package dev.liveeconomy.view.market

import dev.liveeconomy.api.economy.MarketQueryService
import dev.liveeconomy.api.economy.PriceService
import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.core.alert.AlertService
import dev.liveeconomy.util.MoneyFormat
import org.bukkit.entity.Player

/**
 * Builds [MarketView] from services.
 *
 * Owns all derived state previously scattered in [dev.liveeconomy.gui.market.MarketGUI]:
 *  - price lookup per item
 *  - change percent per item
 *  - isPriceUp flag
 *  - category grouping and sorting
 *  - index trend label
 *  - alert count
 *  - player balance
 */
class MarketViewBuilder(
    private val query:    MarketQueryService,
    private val price:    PriceService,
    private val wallet:   WalletService,
    private val alertSvc: AlertService,
    private val symbol:   String
) {
    fun build(player: Player, categoryId: String? = null): MarketView {
        val allItems  = query.getAllItems().values
        val index     = price.getIndex()
        val balance   = wallet.getBalance(player)
        val alertCount = alertSvc.getAlerts(player.uniqueId).size

        val categoryItems = allItems
            .groupBy { it.category.id }
            .mapValues { (_, items) ->
                items.sortedBy { it.itemKey.displayName() }
                    .map { item ->
                        val current   = price.getPrice(item.itemKey) ?: item.basePrice
                        val changePct = price.getPriceChangePercent(item.itemKey) ?: 0.0
                        MarketView.ItemEntry(
                            item         = item,
                            currentPrice = current,
                            changePct    = changePct,
                            isPriceUp    = changePct >= 0
                        )
                    }
            }

        val categories = categoryItems.keys.sorted()

        return MarketView(
            categoryItems    = categoryItems,
            categories       = categories,
            index            = index,
            indexTrend       = MarketView.trendFor(index),
            alertCount       = alertCount,
            balance          = balance,
            balanceFormatted = "$symbol\${MoneyFormat.full(balance)}"
        )
    }
}
