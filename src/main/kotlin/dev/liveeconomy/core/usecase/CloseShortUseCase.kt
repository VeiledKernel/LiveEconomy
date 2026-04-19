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
import java.util.UUID

/**
 * Orchestrates closing a short position with settlement and P&L recording.
 *
 * **No Bukkit imports.** Accepts [UUID] instead of Player.
 * Thread-agnostic — all Bukkit concerns handled by caller.
 *
 * Coordinates [PriceRegistry], [WalletService], [PortfolioStore],
 * [TransactionStore], and [DomainEventBus] (Rule 8 — 3+ collaborators).
 */
class CloseShortUseCase(
    private val registry:  PriceRegistry,
    private val wallet:    WalletService,
    private val portfolio: PortfolioStore,
    private val txStore:   TransactionStore,
    private val eventBus:  DomainEventBus
) {
    fun execute(playerUuid: UUID, item: ItemKey): ShortResult {
        val position   = portfolio.getShortPositions(playerUuid)[item]
            ?: return ShortResult.NoPosition
        val marketItem = registry.getItem(item) ?: return ShortResult.NotListed

        val pnl        = position.unrealisedPnl(marketItem.currentPrice)
        val settlement = (position.collateral + pnl).coerceAtLeast(0.0)

        wallet.deposit(playerUuid, settlement)
        portfolio.removeShortPosition(playerUuid, item)
        portfolio.addPnl(playerUuid, java.math.BigDecimal.valueOf(pnl))

        txStore.append(Transaction(playerUuid, System.currentTimeMillis(), item,
            TradeAction.SHORT_CLOSE, position.quantity, marketItem.currentPrice, pnl))

        eventBus.publish(ShortClosedEvent(playerUuid, item, position.quantity,
            marketItem.currentPrice, pnl))

        return ShortResult.Closed(pnl = pnl)
    }
}
