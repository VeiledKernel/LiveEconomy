package dev.liveeconomy.view.mapper

import dev.liveeconomy.view.alert.AlertView
import dev.liveeconomy.view.alert.AlertViewBuilder
import dev.liveeconomy.view.market.MarketView
import dev.liveeconomy.view.market.MarketViewBuilder
import dev.liveeconomy.view.portfolio.PortfolioView
import dev.liveeconomy.view.portfolio.PortfolioViewBuilder
import dev.liveeconomy.view.wallet.WalletView
import dev.liveeconomy.view.wallet.WalletViewBuilder
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Unified entry point for the view model layer.
 *
 * GUIs depend on [ViewMapper], not on individual builders.
 * This is the single class [GuiFactory] injects into GUI constructors
 * that need view data — replacing the 4 separate builder dependencies.
 *
 * Dependency direction (enforced):
 *   GUI → ViewMapper → Builder → Service → Storage
 *
 * NOT:
 *   GUI → Service  ❌
 *   GUI → Storage  ❌
 *   GUI → Calculations ❌
 */
class ViewMapper(
    private val walletBuilder:    WalletViewBuilder,
    private val portfolioBuilder: PortfolioViewBuilder,
    private val marketBuilder:    MarketViewBuilder,
    private val alertBuilder:     AlertViewBuilder
) {
    fun wallet(player: Player):           WalletView    = walletBuilder.build(player)
    fun portfolio(playerUuid: UUID):      PortfolioView = portfolioBuilder.build(playerUuid)
    fun market(player: Player):           MarketView    = marketBuilder.build(player)
    fun alerts(player: Player):           AlertView     = alertBuilder.build(player)
}
