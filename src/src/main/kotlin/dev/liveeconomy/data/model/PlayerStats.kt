package dev.liveeconomy.data.model

import dev.liveeconomy.api.item.ItemKey

/**
 * Lifetime trading statistics for a single player.
 *
 * Mutable — updated in-place by AnalyticsService on each trade.
 * Thread safety: mutations must be synchronized by the caller (analytics
 * runs on async trade queue; reads happen on main thread for GUI).
 */
data class PlayerStats(
    var totalBuys:   Int    = 0,
    var totalSells:  Int    = 0,
    var wins:        Int    = 0,    // sells that closed above cost basis
    var totalVolume: Double = 0.0,  // cumulative spend + revenue
    var totalRoi:    Double = 0.0   // sum of per-trade ROI %
) {
    /** Fraction of sell trades that were profitable (0.0–1.0). */
    val winRate: Double
        get() = if (totalSells == 0) 0.0 else wins.toDouble() / totalSells

    /** Average ROI per sell trade (%). */
    val avgRoi: Double
        get() = if (totalSells == 0) 0.0 else totalRoi / totalSells

    /** Total number of trades (buys + sells). */
    val totalTrades: Int
        get() = totalBuys + totalSells
}

/**
 * Per-item trading statistics used for leaderboards and price history context.
 */
data class ItemStats(
    val item:       ItemKey,
    var buyVolume:  Double = 0.0,
    var sellVolume: Double = 0.0,
    var buyQty:     Int    = 0,
    var sellQty:    Int    = 0
) {
    val totalVolume: Double get() = buyVolume + sellVolume
    val totalQty:    Int    get() = buyQty + sellQty
}
