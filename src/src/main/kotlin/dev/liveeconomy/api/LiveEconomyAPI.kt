package dev.liveeconomy.api

import dev.liveeconomy.api.economy.MarketQueryService
import dev.liveeconomy.api.economy.PriceService
import dev.liveeconomy.api.economy.TradeService
import dev.liveeconomy.api.player.PortfolioService
import dev.liveeconomy.api.player.WalletService

/**
 * Public entry point for the LiveEconomy API.
 *
 * Obtain an instance with [LiveEconomyAPI.get]:
 * ```kotlin
 * val api = LiveEconomyAPI.get()
 * val price = api.price().getPrice(myItemKey)
 * ```
 *
 * **Stability:**
 * - v4.0 — [ExperimentalLiveEconomyAPI]: opt in required, may change
 * - v4.1 — promoted to stable; no opt-in annotation required
 * - v5.0 — first version where breaking changes are permitted
 *
 * @since 4.0
 */
interface LiveEconomyAPI {

    /**
     * Read-only price queries: current price, bid, ask, index,
     * price change %, and listed items.
     */
    @ExperimentalLiveEconomyAPI
    fun price(): PriceService

    /**
     * Write operations: executeBuy, executeSell, placeLimitOrder,
     * cancelLimitOrder, openShort, closeShort.
     */
    @ExperimentalLiveEconomyAPI
    fun trade(): TradeService

    /**
     * Market data queries: item details, price history, open orders,
     * item statistics, volume rankings.
     */
    @ExperimentalLiveEconomyAPI
    fun query(): MarketQueryService

    /**
     * Player balance operations: getBalance, deposit, withdraw, setBalance.
     */
    @ExperimentalLiveEconomyAPI
    fun wallet(): WalletService

    /**
     * Player portfolio queries: holdings, P&L, stats, shorts, transactions.
     */
    @ExperimentalLiveEconomyAPI
    fun portfolio(): PortfolioService

    companion object {
        @Volatile
        private var instance: LiveEconomyAPI? = null

        /**
         * Returns the active [LiveEconomyAPI] instance.
         * @throws IllegalStateException if LiveEconomy is not loaded.
         */
        fun get(): LiveEconomyAPI =
            instance ?: error(
                "LiveEconomy is not loaded. " +
                "Ensure LiveEconomy is a dependency in your plugin.yml."
            )

        /**
         * Called by LiveEconomy during [org.bukkit.plugin.Plugin.onEnable].
         * Do not call from external plugins.
         */
        internal fun init(api: LiveEconomyAPI) { instance = api }

        /**
         * Called by LiveEconomy during [org.bukkit.plugin.Plugin.onDisable].
         * Do not call from external plugins.
         */
        internal fun clear() { instance = null }
    }
}
