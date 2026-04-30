package dev.liveeconomy.gui.viewmodel

import dev.liveeconomy.api.economy.PriceService
import dev.liveeconomy.api.player.PortfolioService
import dev.liveeconomy.api.storage.TransactionStore
import java.util.UUID

/**
 * Builds [PortfolioView] from services.
 *
 * Owns all derived state that was in [dev.liveeconomy.gui.player.PortfolioGUI]:
 *  - current price lookup per holding
 *  - unrealised P&L per short position
 *  - `isPnlPositive` flag per short
 *  - recent transaction list
 */
class PortfolioViewBuilder(
    private val portfolio: PortfolioService,
    private val price:     PriceService,
    private val txStore:   TransactionStore
) {
    fun build(playerUuid: UUID): PortfolioView {
        val holdings = portfolio.getHoldings(playerUuid)
            .mapValues { (item, qty) ->
                PortfolioView.HoldingEntry(
                    quantity     = qty,
                    currentPrice = price.getPrice(item) ?: 0.0
                )
            }

        val shorts = portfolio.getShortPositions(playerUuid)
            .mapValues { (item, pos) ->
                val current = price.getPrice(item) ?: pos.entryPrice
                val pnl     = pos.unrealisedPnl(current)
                PortfolioView.ShortEntry(
                    position       = pos,
                    currentPrice   = current,
                    unrealisedPnl  = pnl,
                    isPnlPositive  = pnl >= 0
                )
            }

        return PortfolioView(
            pnl        = portfolio.getTotalPnl(playerUuid).toDouble(),
            stats      = portfolio.getStats(playerUuid),
            holdings   = holdings,
            shorts     = shorts,
            recentTxs  = txStore.getRecent(playerUuid, 5)
        )
    }
}
