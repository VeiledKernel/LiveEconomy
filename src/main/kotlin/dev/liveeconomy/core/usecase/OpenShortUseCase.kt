package dev.liveeconomy.core.usecase

import dev.liveeconomy.api.economy.result.ShortResult
import dev.liveeconomy.api.event.DomainEventBus
import dev.liveeconomy.api.event.ShortOpenedEvent
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.api.player.result.WithdrawResult
import dev.liveeconomy.api.storage.PortfolioStore
import dev.liveeconomy.api.storage.TransactionStore
import dev.liveeconomy.core.economy.port.PriceRegistry
import dev.liveeconomy.data.config.MarketConfig
import dev.liveeconomy.data.model.ShortPosition
import dev.liveeconomy.data.model.TradeAction
import dev.liveeconomy.data.model.Transaction
import org.bukkit.entity.Player

/**
 * Orchestrates opening a short position.
 *
 * Coordinates [PriceRegistry], [WalletService], [PortfolioStore],
 * [TransactionStore], and [DomainEventBus] — belongs in a use case (Rule 8).
 *
 * [TradeServiceImpl.openShort] delegates here entirely.
 * Must be called on the main thread.
 */
class OpenShortUseCase(
    private val registry:  PriceRegistry,
    private val wallet:    WalletService,
    private val portfolio: PortfolioStore,
    private val txStore:   TransactionStore,
    private val eventBus:  DomainEventBus,
    private val config:    MarketConfig
) {
    fun execute(player: Player, item: ItemKey, quantity: Int): ShortResult {
        if (!config.allowShortSelling) return ShortResult.Disabled

        val marketItem = registry.getItem(item) ?: return ShortResult.NotListed

        if (portfolio.getShortPositions(player.uniqueId).containsKey(item))
            return ShortResult.AlreadyOpen

        val collateral = marketItem.currentPrice * quantity * config.shortCollateralRatio

        if (wallet.withdraw(player, collateral) is WithdrawResult.InsufficientFunds)
            return ShortResult.InsufficientCollateral

        portfolio.saveShortPosition(ShortPosition(
            playerUuid = player.uniqueId,
            item       = item,
            quantity   = quantity,
            entryPrice = marketItem.currentPrice,
            collateral = collateral
        ))

        txStore.append(Transaction(
            playerUuid = player.uniqueId,
            timestamp  = System.currentTimeMillis(),
            item       = item,
            action     = TradeAction.SHORT_OPEN,
            quantity   = quantity,
            unitPrice  = marketItem.currentPrice,
            total      = collateral
        ))

        eventBus.publish(ShortOpenedEvent(
            playerUuid = player.uniqueId,
            item       = item,
            quantity   = quantity,
            entryPrice = marketItem.currentPrice,
            collateral = collateral
        ))

        return ShortResult.Opened(collateral = collateral, entryPrice = marketItem.currentPrice)
    }
}
