package dev.liveeconomy.core.usecase

import dev.liveeconomy.api.economy.result.ShortResult
import dev.liveeconomy.api.event.DomainEventBus
import dev.liveeconomy.api.event.ShortClosedEvent
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.api.storage.PortfolioStore
import dev.liveeconomy.api.storage.TransactionStore
import dev.liveeconomy.core.economy.port.PriceRegistry
import dev.liveeconomy.data.model.TradeAction
import dev.liveeconomy.data.model.Transaction
import org.bukkit.entity.Player

/**
 * Orchestrates closing a short position with settlement, P&L recording,
 * and event publication.
 *
 * Coordinates [PriceRegistry], [WalletService], [PortfolioStore],
 * [TransactionStore], and [DomainEventBus] — belongs in a use case (Rule 8).
 *
 * [TradeServiceImpl.closeShort] delegates here entirely.
 * Must be called on the main thread.
 */
class CloseShortUseCase(
    private val registry:  PriceRegistry,
    private val wallet:    WalletService,
    private val portfolio: PortfolioStore,
    private val txStore:   TransactionStore,
    private val eventBus:  DomainEventBus
) {
    fun execute(player: Player, item: ItemKey): ShortResult {
        val position   = portfolio.getShortPositions(player.uniqueId)[item]
            ?: return ShortResult.NoPosition
        val marketItem = registry.getItem(item) ?: return ShortResult.NotListed

        val pnl        = position.unrealisedPnl(marketItem.currentPrice)
        val settlement = (position.collateral + pnl).coerceAtLeast(0.0)

        // Settle: return collateral ± P&L
        wallet.deposit(player, settlement)

        portfolio.removeShortPosition(player.uniqueId, item)
        portfolio.addPnl(player.uniqueId, java.math.BigDecimal.valueOf(pnl))

        txStore.append(Transaction(
            playerUuid = player.uniqueId,
            timestamp  = System.currentTimeMillis(),
            item       = item,
            action     = TradeAction.SHORT_CLOSE,  // correct — distinct from SELL
            quantity   = position.quantity,
            unitPrice  = marketItem.currentPrice,
            total      = pnl
        ))

        eventBus.publish(ShortClosedEvent(
            playerUuid = player.uniqueId,
            item       = item,
            quantity   = position.quantity,
            exitPrice  = marketItem.currentPrice,
            pnl        = pnl
        ))

        return ShortResult.Closed(pnl = pnl)
    }
}
