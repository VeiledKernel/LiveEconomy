package dev.liveeconomy.core.usecase

import dev.liveeconomy.api.economy.result.ShortResult
import dev.liveeconomy.api.event.DomainEventBus
import dev.liveeconomy.api.event.ShortOpenedEvent
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.api.storage.PortfolioStore
import dev.liveeconomy.api.storage.TransactionStore
import dev.liveeconomy.core.economy.port.PriceRegistry
import dev.liveeconomy.data.config.MarketConfig
import dev.liveeconomy.data.model.ShortPosition
import dev.liveeconomy.data.model.TradeAction
import dev.liveeconomy.data.model.Transaction
import java.util.UUID

/**
 * Orchestrates opening a short position.
 *
 * **No Bukkit imports.** Accepts [UUID] instead of Player.
 * Thread-agnostic — all Bukkit concerns handled by caller.
 *
 * Coordinates [PriceRegistry], [WalletService], [PortfolioStore],
 * [TransactionStore], and [DomainEventBus] (Rule 8 — 3+ collaborators).
 */
class OpenShortUseCase(
    private val registry:  PriceRegistry,
    private val wallet:    WalletService,
    private val portfolio: PortfolioStore,
    private val txStore:   TransactionStore,
    private val eventBus:  DomainEventBus,
    private val config:    MarketConfig
) {
    fun execute(playerUuid: UUID, item: ItemKey, quantity: Int): ShortResult {
        if (!config.allowShortSelling) return ShortResult.Disabled

        val marketItem = registry.getItem(item) ?: return ShortResult.NotListed

        if (portfolio.getShortPositions(playerUuid).containsKey(item))
            return ShortResult.AlreadyOpen

        val collateral = marketItem.currentPrice * quantity * config.shortCollateralRatio

        if (!wallet.has(playerUuid, collateral))
            return ShortResult.InsufficientCollateral

        wallet.withdraw(playerUuid, collateral)

        portfolio.saveShortPosition(ShortPosition(
            playerUuid = playerUuid,
            item       = item,
            quantity   = quantity,
            entryPrice = marketItem.currentPrice,
            collateral = collateral
        ))

        txStore.append(Transaction(playerUuid, System.currentTimeMillis(), item,
            TradeAction.SHORT_OPEN, quantity, marketItem.currentPrice, collateral))

        eventBus.publish(ShortOpenedEvent(playerUuid, item, quantity,
            marketItem.currentPrice, collateral))

        return ShortResult.Opened(collateral = collateral, entryPrice = marketItem.currentPrice)
    }
}
