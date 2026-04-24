package dev.liveeconomy.platform

import dev.liveeconomy.api.economy.MarketQueryService
import dev.liveeconomy.api.economy.PriceService
import dev.liveeconomy.api.economy.TradeService
import dev.liveeconomy.api.player.PortfolioService
import dev.liveeconomy.api.player.WalletService

/**
 * Last-resort service locator for Bukkit-managed entry points where
 * constructor injection is impossible.
 *
 * **ONLY permitted in (DI-RULES.md Rule 5):**
 *  1. PlaceholderAPI — instantiates via reflection
 *  2. Third-party APIs that own object construction
 *  3. Legacy compatibility bridges
 *
 * Populated LAST in [dev.liveeconomy.LiveEconomy.onEnable].
 * Cleared on [dev.liveeconomy.LiveEconomy.onDisable].
 *
 * Forbidden in: core/, gui/, command/, storage/, data/, api/, util/
 */
object ServiceLocator {

    @Volatile lateinit var price:     PriceService      private set
    @Volatile lateinit var trade:     TradeService      private set
    @Volatile lateinit var query:     MarketQueryService private set
    @Volatile lateinit var wallet:    WalletService     private set
    @Volatile lateinit var portfolio: PortfolioService  private set

    fun init(
        price:     PriceService,
        trade:     TradeService,
        query:     MarketQueryService,
        wallet:    WalletService,
        portfolio: PortfolioService
    ) {
        this.price     = price
        this.trade     = trade
        this.query     = query
        this.wallet    = wallet
        this.portfolio = portfolio
    }

    fun isInitialized(): Boolean = ::price.isInitialized
}
