package dev.liveeconomy.core.market

import dev.liveeconomy.api.event.DomainEventBus
import dev.liveeconomy.api.event.PriceChangedEvent
import dev.liveeconomy.api.storage.PriceStore
import dev.liveeconomy.core.economy.PriceModelImpl
import dev.liveeconomy.core.economy.TradeServiceImpl
import dev.liveeconomy.data.config.MarketConfig

/**
 * Drives the market tick — price updates, mean reversion, candle recording.
 *
 * Extracted from [dev.liveeconomy.core.economy.PriceServiceImpl] to give
 * tick lifecycle its own class with a single responsibility.
 *
 * Called by [dev.liveeconomy.platform.scheduler.MarketTickTask] on the
 * main thread at the configured interval.
 *
 * // No interface: internal tick engine, single implementation.
 */
class MarketTicker(
    private val registry:  MarketRegistry,
    private val model:     PriceModelImpl,
    private val trade:     TradeServiceImpl,
    private val store:     PriceStore,
    private val eventBus:  DomainEventBus,
    private val config:    MarketConfig
) {
    /**
     * Execute one full market tick:
     *  1. Open tick ranges on all items
     *  2. Apply mean reversion
     *  3. Recalculate volatility
     *  4. Process triggered limit orders
     *  5. Record OHLCV candle
     *  6. Publish price change events for significant moves
     *  7. Close tick (reset trade counters)
     */
    fun tick() {
        val items = registry.allItems()

        for (item in items) {
            val prevPrice = item.currentPrice
            item.openTick()

            // Mean reversion — drift price back toward base
            model.applyMeanReversion(item)
            model.recalculateVolatility(item)

            // Process any limit orders triggered by new price
            trade.processTriggeredOrders(item.itemKey, item.currentPrice)

            // Record candle
            store.appendCandle(item.itemKey, dev.liveeconomy.data.model.PriceCandle(
                open   = item.tickOpen,
                high   = item.tickHigh,
                low    = item.tickLow,
                close  = item.currentPrice,
                volume = item.tradeCount.toDouble()
            ))

            // Broadcast significant price changes
            val changePct = item.priceChangePercent
            if (Math.abs(changePct) >= 0.5 && item.currentPrice != prevPrice) {
                eventBus.publish(PriceChangedEvent(
                    item          = item.itemKey,
                    previousPrice = prevPrice,
                    newPrice      = item.currentPrice,
                    changePercent = changePct
                ))
            }

            item.resetTradeCount()
        }
    }

    /** Apply idle decay to items with no trades — called on decay schedule. */
    fun applyIdleDecay() {
        registry.allItems()
            .filter { it.tradeCount == 0 }
            .forEach { model.applyIdleDecay(it) }
    }
}
