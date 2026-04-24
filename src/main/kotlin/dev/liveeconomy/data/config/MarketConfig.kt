package dev.liveeconomy.data.config

/**
 * Typed configuration for market engine settings.
 */
data class MarketConfig(
    val tickIntervalTicks:        Long,
    val baseLiquidity:            Double,
    val reversionStrength:        Double,
    val idleDecay:                Double,
    val broadcastThreshold:       Double,
    val tickerPermissionRequired: Boolean,
    val allowShortSelling:        Boolean,
    val shortCollateralRatio:     Double,
    val tradeTaxPercent:          Double,
    val decayIntervalMinutes:     Long,
    val alertMaxPerPlayer:        Int,
    val alertEnabled:             Boolean,
    val marginEnabled:            Boolean,
    val marginCallLevel:          Double,
    val marginLiquidationLevel:   Double
) {
    /** Trade tax as a 0.0–1.0 fraction (e.g. 2.0% → 0.02) */
    val tradeTaxRate: Double get() = tradeTaxPercent / 100.0
}
    /** Trade tax as a decimal rate (e.g. 2.0% → 0.02). */
    val tradeTaxRate: Double get() = tradeTaxPercent / 100.0

    companion object {
        val DEFAULT = MarketConfig(
            tickIntervalTicks        = 200L,
            baseLiquidity            = 100.0,
            reversionStrength        = 0.02,
            idleDecay                = 0.05,
            broadcastThreshold       = 500.0,
            tickerPermissionRequired = false,
            allowShortSelling        = true,
            shortCollateralRatio     = 1.5,
            tradeTaxPercent          = 2.0,
            decayIntervalMinutes     = 5L,
            alertMaxPerPlayer        = 5,
            alertEnabled             = true
        )
    }
}