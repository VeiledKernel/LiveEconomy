package dev.liveeconomy.data.config

/**
 * Typed configuration for the prestige system.
 */
data class PrestigeConfig(
    val enabled:             Boolean,
    val requiredPnl:         Double,
    val maxLevel:            Int,
    val tradeBonusPercent:   Double,   // % sell bonus per level
    val taxReductionPercent: Double,   // % tax reduction per level
    val alertLimitBonus:     Int       // extra alert slots per level
) {
    /** Trade bonus as decimal per level (e.g. 2.0% → 0.02). */
    val tradeBonusPerLevel: Double get() = tradeBonusPercent / 100.0

    /** Tax reduction as decimal per level (e.g. 5.0% → 0.05). */
    val taxReductionPerLevel: Double get() = taxReductionPercent / 100.0

    /** Total sell bonus multiplier at [level]. Stacks additively. */
    fun sellBonusAt(level: Int): Double = 1.0 + tradeBonusPerLevel * level

    /** Effective tax discount multiplier at [level]. */
    fun taxDiscountAt(level: Int): Double =
        (1.0 - taxReductionPerLevel * level).coerceAtLeast(0.0)

    companion object {
        val DEFAULT = PrestigeConfig(
            enabled             = true,
            requiredPnl         = 100_000.0,
            maxLevel            = 10,
            tradeBonusPercent   = 2.0,
            taxReductionPercent = 5.0,
            alertLimitBonus     = 1
        )
    }
}
