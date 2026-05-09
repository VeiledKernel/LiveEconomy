package dev.liveeconomy.integration.papi

import dev.liveeconomy.api.Lifecycle
import dev.liveeconomy.platform.ServiceLocator
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer

/**
 * PlaceholderAPI expansion for LiveEconomy.
 *
 * Registers the following read-only placeholders:
 *
 * | Placeholder                       | Returns                              |
 * |-----------------------------------|--------------------------------------|
 * | `%liveeconomy_balance%`           | Raw balance as double string         |
 * | `%liveeconomy_balance_formatted%` | Formatted balance with symbol        |
 * | `%liveeconomy_market_index%`      | Market index value (1 decimal)       |
 * | `%liveeconomy_role%`              | Player's current trader role name    |
 * | `%liveeconomy_portfolio_value%`   | Total holdings value (compact)       |
 * | `%liveeconomy_pnl%`               | Lifetime P&L with sign               |
 * | `%liveeconomy_top_item%`          | Highest-priced item display name     |
 *
 * Rules:
 *  - Read-only: no write operations, no trading, no state mutation.
 *  - Fail gracefully: returns "" if player is null or service unavailable.
 *  - Never throws: all resolution wrapped in runCatching.
 *  - No business logic: all derived values come from services — no inline
 *    calculations, no sorting, no aggregation in this class.
 *
 * // ServiceLocator used: PlaceholderAPI owns expansion lifecycle and does not
 * // support normal constructor injection.
 * // Approved third-party lifecycle exception. (DI-RULES.md Rule 5)
 *
 * Implements [Lifecycle] so [dev.liveeconomy.PluginBoot] can unregister on shutdown.
 */
class LiveEconomyExpansion : PlaceholderExpansion(), Lifecycle {

    override fun getIdentifier() = "liveeconomy"
    override fun getAuthor()     = "NexaStudios"
    override fun getVersion()    = "4.0"
    override fun persist()       = true
    override fun canRegister()   = true

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun start() {
        if (isRegistered) return
        register()
    }

    override fun stop() {
        if (isRegistered) unregister()
    }

    // ── Placeholder resolution ─────────────────────────────────────────────────

    override fun onRequest(player: OfflinePlayer?, params: String): String {
        if (player == null) return ""
        if (!ServiceLocator.isInitialized()) return ""

        return runCatching {
            val uuid = player.uniqueId
            when (params.lowercase()) {

                "balance" ->
                    ServiceLocator.wallet.getBalance(uuid).toString()

                "balance_formatted" ->
                    ServiceLocator.wallet.getBalanceFormatted(uuid)

                "market_index" ->
                    String.format("%.1f", ServiceLocator.price.getIndex())

                "role" ->
                    ServiceLocator.portfolio.getRole(uuid).displayName

                // Delegates to PortfolioService — no inline calculation here
                "portfolio_value" ->
                    ServiceLocator.portfolio.getPortfolioValueFormatted(uuid)

                // Delegates to PortfolioService — no inline P&L calculation here
                "pnl" ->
                    ServiceLocator.portfolio.getPnlFormatted(uuid)

                // Delegates to MarketQueryService — no inline sorting/aggregation here
                "top_item" ->
                    ServiceLocator.query.getTopPricedItem()?.displayName() ?: ""

                else -> ""
            }
        }.getOrElse { "" }
    }
}
