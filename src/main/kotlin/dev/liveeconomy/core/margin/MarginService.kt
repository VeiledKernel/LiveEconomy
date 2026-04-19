package dev.liveeconomy.core.margin

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.api.scheduler.Scheduler
import dev.liveeconomy.api.storage.PortfolioStore
import dev.liveeconomy.core.economy.PriceServiceImpl
import dev.liveeconomy.data.config.MarketConfig
import dev.liveeconomy.core.usecase.port.PlayerResolver
import java.util.UUID

/**
 * Monitors open short positions for margin calls and forced liquidation.
 *
 * Called periodically by [MarketTickTask] — checks margin levels and
 * liquidates positions that breach [MarketConfig.marginLiquidationLevel].
 *
 * // No interface: single implementation, internal engine concern.
 */
class MarginService(
    private val prices:    PriceServiceImpl,
    private val portfolio: PortfolioStore,
    private val wallet:    WalletService,
    private val config:    MarketConfig,
    private val scheduler: Scheduler,
    private val playerResolver: PlayerResolver
) {
    fun checkAllMargins() {
        val allShorts = portfolio.getAllShortPositions()

        for ((uuid, positions) in allShorts) {
            for ((item, position) in positions) {
                val marketItem = prices.getItem(item) ?: continue
                val marginPct  = position.marginLevel(marketItem.currentPrice)

                when {
                    marginPct <= config.marginLiquidationLevel -> liquidate(uuid, item)
                    marginPct <= config.marginCallLevel        -> warnMarginCall(uuid, item, marginPct)
                }
            }
        }
    }

    private fun liquidate(uuid: UUID, item: ItemKey) {
        val position   = portfolio.getShortPositions(uuid)[item] ?: return
        val marketItem = prices.getItem(item) ?: return
        val pnl        = position.unrealisedPnl(marketItem.currentPrice)
        val settlement = (position.collateral + pnl).coerceAtLeast(0.0)

        portfolio.removeShortPosition(uuid, item)
        portfolio.addPnl(uuid, java.math.BigDecimal.valueOf(pnl))

        wallet.deposit(uuid, settlement)
        scheduler.runOnMain {
            playerResolver.withOnlinePlayer(uuid) {
                sendMessage("§8[§6§lMarket§8] §r§c⚠ Margin liquidation: §f${item.displayName()} §cposition force-closed.")
            }
        }
    }

    private fun warnMarginCall(uuid: UUID, item: ItemKey, marginPct: Double) {
        scheduler.runOnMain {
            playerResolver.withOnlinePlayer(uuid) {
                sendMessage(
                    "§8[§6§lMarket§8] §r§e⚠ Margin call on §f${item.displayName()}§e — " +
                    "margin level ${String.format("%.0f", marginPct)}%%. Add funds or close."
                )
            }
        }
    }
}
