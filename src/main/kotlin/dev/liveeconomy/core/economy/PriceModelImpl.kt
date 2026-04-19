package dev.liveeconomy.core.economy

import dev.liveeconomy.api.extension.PriceModifier
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.data.config.MarketConfig
import dev.liveeconomy.data.model.MarketItem
import kotlin.math.exp
import kotlin.math.ln

/**
 * Core price calculation engine.
 *
 * Implements supply/demand pricing, bid/ask spread, mean reversion,
 * and volatility tracking. Holds a [List<PriceModifier>] extension point —
 * add new price rules by registering a modifier at the composition root,
 * never by editing this class (Open/Closed Principle).
 *
 * Thread safety: called from the async market tick thread. All reads from
 * [MarketItem] use @Volatile fields. Writes to item price state are
 * performed here and are safe because the tick runs on a single thread.
 *
 * // No interface: internal pricing engine — not part of public api/
 */
class PriceModelImpl(
    private val config:    MarketConfig,
    private val modifiers: List<PriceModifier> = emptyList()
) {

    // ── Bid / Ask spread ──────────────────────────────────────────────────────

    /**
     * Bid price — highest a buyer will pay. Slightly below mid-market.
     * Spread widens as volatility increases.
     */
    fun bid(item: MarketItem): Double =
        item.currentPrice * (1.0 - spreadHalf(item.volatility))

    /**
     * Ask price — lowest a seller will accept. Slightly above mid-market.
     */
    fun ask(item: MarketItem): Double =
        item.currentPrice * (1.0 + spreadHalf(item.volatility))

    private fun spreadHalf(volatility: Double): Double =
        (volatility * 0.5).coerceIn(0.001, 0.05)

    // ── Price impact ──────────────────────────────────────────────────────────

    /**
     * Apply buy pressure to [item] — pushes price up.
     * Impact diminishes as [config.baseLiquidity] increases.
     *
     * @param quantity units bought
     * @param taxRate  effective tax rate (0.0–1.0)
     * @return cost including tax, after price impact is applied
     */
    fun applyBuyImpact(item: MarketItem, quantity: Int, taxRate: Double): Double {
        val preTaxCost = item.askPrice * quantity
        val impact = quantity.toDouble() / config.baseLiquidity
        val newPrice = (item.currentPrice * exp(impact))
            .coerceIn(item.minPrice, item.maxPrice)

        applyModifiers(item, newPrice)
        item.updateTickRange(newPrice)
        item.incrementTradeCount()

        return preTaxCost * (1.0 + taxRate)
    }

    /**
     * Apply sell pressure to [item] — pushes price down.
     *
     * @param quantity units sold
     * @param taxRate  effective tax rate (0.0–1.0)
     * @return revenue after tax deduction
     */
    fun applySellImpact(item: MarketItem, quantity: Int, taxRate: Double): Double {
        val preTaxRevenue = item.bidPrice * quantity
        val impact = quantity.toDouble() / config.baseLiquidity
        val newPrice = (item.currentPrice * exp(-impact))
            .coerceIn(item.minPrice, item.maxPrice)

        applyModifiers(item, newPrice)
        item.updateTickRange(newPrice)
        item.incrementTradeCount()

        return preTaxRevenue * (1.0 - taxRate)
    }

    /**
     * Apply a shock to an entire category — percentage price movement.
     * Positive [percent] = price rise, negative = price fall.
     */
    fun applyShock(item: MarketItem, percent: Double) {
        val factor = 1.0 + (percent / 100.0)
        val newPrice = (item.currentPrice * factor).coerceIn(item.minPrice, item.maxPrice)
        applyModifiers(item, newPrice)
    }

    // ── Mean reversion ────────────────────────────────────────────────────────

    /**
     * Drift current price toward [item.basePrice] by [config.reversionStrength].
     * Called once per market tick for each item regardless of trade activity.
     */
    fun applyMeanReversion(item: MarketItem) {
        val gap = item.basePrice - item.currentPrice
        val newPrice = (item.currentPrice + gap * config.reversionStrength)
            .coerceIn(item.minPrice, item.maxPrice)
        item.currentPrice = newPrice
        item.bidPrice     = bid(item)
        item.askPrice     = ask(item)
    }

    /**
     * Decay price toward base when the item has had no trades this tick.
     * Stronger than mean reversion — used on the idle decay schedule.
     */
    fun applyIdleDecay(item: MarketItem) {
        val gap = item.basePrice - item.currentPrice
        val newPrice = (item.currentPrice + gap * config.idleDecay)
            .coerceIn(item.minPrice, item.maxPrice)
        item.currentPrice = newPrice
        item.bidPrice     = bid(item)
        item.askPrice     = ask(item)
    }

    // ── Volatility ────────────────────────────────────────────────────────────

    /**
     * Recalculate [item.volatility] based on recent price history.
     * Higher trade volume → higher volatility.
     */
    fun recalculateVolatility(item: MarketItem) {
        if (item.previousPrice == 0.0) return
        val logReturn = ln(item.currentPrice / item.previousPrice)
        // Exponential moving average of |log returns|
        item.volatility = (item.volatility * 0.95 + Math.abs(logReturn) * 0.05)
            .coerceIn(0.001, 0.5)
    }

    // ── Market index ──────────────────────────────────────────────────────────

    /**
     * Compute composite market index from [items].
     * 1000.0 = all items at base price. >1000 = bull, <1000 = bear.
     */
    fun computeIndex(items: Collection<MarketItem>): Double {
        if (items.isEmpty()) return 1000.0
        return items.map { it.currentPrice / it.basePrice }.average() * 1000.0
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun applyModifiers(item: MarketItem, baseNewPrice: Double) {
        var price = baseNewPrice
        for (modifier in modifiers) {
            price = modifier.modify(item.itemKey, price)
        }
        item.currentPrice = price.coerceIn(item.minPrice, item.maxPrice)
        item.bidPrice     = bid(item)
        item.askPrice     = ask(item)
    }
}
