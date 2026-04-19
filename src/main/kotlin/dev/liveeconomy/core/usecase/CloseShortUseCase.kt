package dev.liveeconomy.core.usecase

import dev.liveeconomy.api.economy.TradeService
import dev.liveeconomy.api.economy.result.ShortResult
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.storage.PortfolioStore
import dev.liveeconomy.api.storage.TransactionStore
import dev.liveeconomy.data.model.TradeAction
import dev.liveeconomy.data.model.Transaction
import org.bukkit.entity.Player

/**
 * Orchestrates closing a short position with P&L recording.
 *
 * Coordinates [TradeService] and [TransactionStore] — multi-service
 * operation that belongs in a use case (Rule 8).
 */
class CloseShortUseCase(
    private val trade:    TradeService,
    private val txStore:  TransactionStore,
    private val portfolio: PortfolioStore
) {
    fun execute(player: Player, item: ItemKey): ShortResult {
        val result = trade.closeShort(player, item)
        if (result is ShortResult.Closed) {
            txStore.append(
                Transaction(
                    playerUuid = player.uniqueId,
                    timestamp  = System.currentTimeMillis(),
                    item       = item,
                    action     = TradeAction.SELL,   // short close = selling borrowed position
                    quantity   = 0,                  // quantity not tracked at close for shorts
                    unitPrice  = result.pnl,
                    total      = result.pnl
                )
            )
            portfolio.addPnl(player.uniqueId, java.math.BigDecimal.valueOf(result.pnl))
        }
        return result
    }
}
