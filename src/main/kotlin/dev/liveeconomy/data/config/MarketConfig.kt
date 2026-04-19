package dev.liveeconomy.data.config

/**
 * Typed representation of the `market:` block in config.yml.
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
    val decayIntervalMinutes:     Long
)
