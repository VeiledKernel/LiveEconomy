package dev.liveeconomy.core.usecase

import dev.liveeconomy.api.economy.TradeService
import dev.liveeconomy.api.economy.result.TradeResult
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.storage.TransactionStore
import dev.liveeconomy.data.model.TradeAction
import dev.liveeconomy.data.model.Transaction
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Orchestrates a complete market trade — execution + transaction recording.
 *
 * Coordinates [TradeService] and [TransactionStore] so neither needs to know
 * about the other. This is the correct place for multi-service coordination
 * (Rule 8 — Use Cases for 3+ service operations).
 *
 * Called by: [dev.liveeconomy.command.market.sub.BuySubCommand],
 *            [dev.liveeconomy.gui.market.quantity.QuantitySelectorGUI]
 */
class ExecuteTradeUseCase(
    private val trade:    TradeService,
    private val txStore:  TransactionStore
) {
    fun executeBuy(player: Player, item: ItemKey, quantity: Int): TradeResult {
        val result = trade.executeBuy(player, item, quantity)
        if (result is TradeResult.Success) {
            txStore.append(
                Transaction(
                    playerUuid = player.uniqueId,
                    timestamp  = System.currentTimeMillis(),
                    item       = item,
                    action     = TradeAction.BUY,
                    quantity   = quantity,
                    unitPrice  = result.total / quantity,
                    total      = result.total
                )
            )
        }
        return result
    }

    fun executeSell(player: Player, item: ItemKey, quantity: Int): TradeResult {
        val result = trade.executeSell(player, item, quantity)
        if (result is TradeResult.Success) {
            txStore.append(
                Transaction(
                    playerUuid = player.uniqueId,
                    timestamp  = System.currentTimeMillis(),
                    item       = item,
                    action     = TradeAction.SELL,
                    quantity   = quantity,
                    unitPrice  = result.total / quantity,
                    total      = result.total
                )
            )
        }
        return result
    }
}
