package dev.liveeconomy.data.model

import dev.liveeconomy.api.item.ItemKey

/**
 * A tradable item registered in the market.
 *
 * Mutable price state uses @Volatile so async tick reads never see stale
 * values. Trade count uses AtomicInteger to prevent lost updates when
 * multiple trades arrive in the same tick.
 *
 * Uses [ItemKey] for stable item identity — no direct Material dependency.
 */
class MarketItem(
    val itemKey:       ItemKey,
    val basePrice:     Double,
    val baseVolatility: Double,
    val category:      MarketCategory,
    val minPrice:      Double,
    val maxPrice:      Double,
    val displayName:   String = itemKey.displayName()
) {
    // ── Volatile price state — safe for async reads ───────────────────────────
    @Volatile var currentPrice:  Double = basePrice
    @Volatile var bidPrice:      Double = basePrice * 0.98
    @Volatile var askPrice:      Double = basePrice * 1.02
    @Volatile var volatility:    Double = baseVolatility
    @Volatile var previousPrice: Double = basePrice

    // ── Per-tick OHLCV ────────────────────────────────────────────────────────
    @Volatile var tickOpen: Double = basePrice; private set
    @Volatile var tickHigh: Double = basePrice; private set
    @Volatile var tickLow:  Double = basePrice; private set

    // ── Atomic trade counter ──────────────────────────────────────────────────
    private val tradeCounter = java.util.concurrent.atomic.AtomicInteger(0)
    val tradeCount: Int get() = tradeCounter.get()
    fun incrementTradeCount() { tradeCounter.incrementAndGet() }

    // ── Tick lifecycle ────────────────────────────────────────────────────────
    fun openTick() {
        tickOpen = currentPrice; tickHigh = currentPrice; tickLow = currentPrice
    }

    fun updateTickRange(price: Double) {
        if (price > tickHigh) tickHigh = price
        if (price < tickLow)  tickLow  = price
    }

    fun resetTradeCount() {
        previousPrice = currentPrice
        tradeCounter.set(0)
    }

    // ── Computed ──────────────────────────────────────────────────────────────
    val priceChangePercent: Double
        get() = if (previousPrice == 0.0) 0.0
                else ((currentPrice - previousPrice) / previousPrice) * 100.0

    val isAboveBase: Boolean get() = currentPrice > basePrice
    val isBelowBase: Boolean get() = currentPrice < basePrice

    override fun equals(other: Any?): Boolean =
        other is MarketItem && itemKey.id == other.itemKey.id

    override fun hashCode(): Int = itemKey.id.hashCode()

    override fun toString(): String = "MarketItem(${itemKey.id}, price=$currentPrice)"
}
