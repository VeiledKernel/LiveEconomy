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
 * GUIs depend on [ViewMapper] — not on individual builders.
 *
 * **DEBT-VM-1: ViewMapper split**
 * This class will become a god-class as more screens are added.
 * Future target structure:
 * ```
 * view/mapper/
 *   WalletViewMapper.kt
 *   MarketViewMapper.kt
 *   PortfolioViewMapper.kt
 *   AlertViewMapper.kt
 *   ViewMapper.kt  ← facade only, delegates to sub-mappers
 * ```
 * Defer until 5+ screen types are wired, or ViewMapper exceeds 100 lines.
 *
 * **DEBT-VM-2: MarketView size**
 * MarketView may need splitting into MarketItemView / MarketStatsView /
 * MarketPageView if it grows beyond items + index + balance.
 * Monitor when pagination state is added in Phase 6.
 *
 * **DEBT-VM-3: ViewMapper computation cost**
 * MarketViewBuilder rebuilds the full item list on every GUI open.
 * For servers with 100+ market items, add a caching layer with
 * invalidation on price tick. Defer until profiling shows lag.
 */
class ViewMapper(
    private val walletBuilder:    WalletViewBuilder,
    private val portfolioBuilder: PortfolioViewBuilder,
    private val marketBuilder:    MarketViewBuilder,
    private val alertBuilder:     AlertViewBuilder
) {
    fun wallet(player: Player):       WalletView    = walletBuilder.build(player)
    fun portfolio(playerUuid: UUID):  PortfolioView = portfolioBuilder.build(playerUuid)
    fun market(player: Player):       MarketView    = marketBuilder.build(player)
    fun alerts(player: Player):       AlertView     = alertBuilder.build(player)
}
