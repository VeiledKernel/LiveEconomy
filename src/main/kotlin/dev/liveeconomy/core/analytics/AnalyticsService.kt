package dev.liveeconomy.core.analytics

import dev.liveeconomy.api.event.DomainEvent
import dev.liveeconomy.api.event.DomainEventHandler
import dev.liveeconomy.api.event.TradeExecutedEvent
import dev.liveeconomy.api.scheduler.Scheduler
import dev.liveeconomy.api.storage.PortfolioStore
import dev.liveeconomy.data.model.TradeAction

/**
 * Records per-player trading statistics from [TradeExecutedEvent].
 *
 * Subscribes to the [DomainEventBus] — fired synchronously after each trade.
 * Heavy storage writes are dispatched async via [Scheduler].
 */
class AnalyticsService(
    private val store:     PortfolioStore,
    private val scheduler: Scheduler
) : DomainEventHandler {

    override fun handle(event: DomainEvent) {
        if (event !is TradeExecutedEvent) return
        scheduler.runAsync { record(event) }
    }

    private fun record(event: TradeExecutedEvent) {
        val stats = store.getStats(event.playerUuid)

        when (event.action) {
            TradeAction.BUY -> {
                stats.totalBuys++
                stats.totalVolume += event.total
            }
            TradeAction.SELL -> {
                stats.totalSells++
                stats.totalVolume += event.total
                // A sell is a "win" if revenue exceeded the item's current base
                // (simplified — full cost-basis tracking is a future feature)
                if (event.total > 0) stats.wins++
                stats.totalRoi += (event.total / event.unitPrice - 1.0) * 100.0
            }
        }

        store.saveStats(event.playerUuid, stats)
    }
}
