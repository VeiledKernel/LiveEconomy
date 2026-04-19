package dev.liveeconomy.core.usecase

import dev.liveeconomy.api.economy.result.ShortResult
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.api.player.result.WithdrawResult
import dev.liveeconomy.api.storage.PortfolioStore
import dev.liveeconomy.core.economy.port.PriceRegistry
import dev.liveeconomy.data.config.MarketConfig
import dev.liveeconomy.data.model.ShortPosition
import org.bukkit.entity.Player

/**
 * Orchestrates opening a short position.
 *
 * Coordinates [PriceRegistry], [WalletService], and [PortfolioStore] —
 * three collaborators, belongs in a use case (Rule 8).
 *
 * [TradeServiceImpl.openShort] delegates to this entirely.
 */
class OpenShortUseCase(
    private val registry:  PriceRegistry,
    private val wallet:    WalletService,
    private val portfolio: PortfolioStore,
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

        return ShortResult.Opened(collateral = collateral, entryPrice = marketItem.currentPrice)
    }
}
