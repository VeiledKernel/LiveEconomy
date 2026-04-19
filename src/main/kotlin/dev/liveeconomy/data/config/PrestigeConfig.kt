package dev.liveeconomy.data.config

/**
 * Typed representation of the `prestige:` block in config.yml.
 */
data class PrestigeConfig(
    val enabled:              Boolean,
    val requiredPnl:          Double,
    val maxLevel:             Int,
    val tradeBonusPercent:    Double,  // per level — applied as fraction (2.0 → 0.02)
    val taxReductionPercent:  Double,  // per level — applied as fraction
    val alertLimitBonus:      Int
)
