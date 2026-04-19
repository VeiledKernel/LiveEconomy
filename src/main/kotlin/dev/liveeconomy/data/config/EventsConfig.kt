package dev.liveeconomy.data.config

/**
 * Typed representation of the `events:` block in config.yml.
 *
 * Each shock type has its own nested config object so the composition root
 * can pass only what a given ShockHandler needs.
 */
data class EventsConfig(
    val enabled:       Boolean,
    val intervalTicks: Long,
    val cooldowns:     Cooldowns,
    val blockBreak:    ShockConfig,
    val crafting:      ShockConfig,
    val fishing:       FishingConfig,
    val enchanting:    EnchantingConfig,
    val mining:        ThresholdShockConfig,
    val harvest:       CategoryShockConfig,
    val bulkCrafting:  BulkCraftingConfig,
    val raid:          RaidConfig,
    val bossKills:     BossKillsConfig,
    val nightCycle:    NightCycleConfig,
    val deathSpree:    ThresholdShockConfig,
    val massActivity:  MassActivityConfig
) {
    data class Cooldowns(
        val default:    Int,
        val fishing:    Int,
        val enchanting: Int,
        val nightCycle: Int
    )

    data class ShockConfig(
        val enabled:         Boolean,
        val impactMultiplier: Double
    )

    data class FishingConfig(
        val enabled:          Boolean,
        val impactMultiplier: Double,
        val regularShock:     Double,
        val treasureShock:    Double
    )

    data class EnchantingConfig(
        val enabled:     Boolean,
        val minLevel:    Int,
        val lapImpact:   Double,
        val shockPct:    Double,
        val category:    String,
        val message:     String
    )

    data class ThresholdShockConfig(
        val enabled:       Boolean,
        val threshold:     Int,
        val windowSeconds: Int,
        val shockPercent:  Double,
        val category:      String = "",
        val message:       String = ""
    )

    data class CategoryShockConfig(
        val enabled:       Boolean,
        val threshold:     Int,
        val windowSeconds: Int,
        val shockPercent:  Double,
        val category:      String,
        val message:       String
    )

    data class BulkCraftingConfig(
        val enabled:       Boolean,
        val threshold:     Int,
        val windowSeconds: Int,
        val shockPercent:  Double,
        val messagePrefix: String
    )

    data class RaidConfig(
        val enabled:       Boolean,
        val bonusPercent:  Double
    )

    data class BossKillsConfig(
        val enabled: Boolean,
        val dragon:  BossShock,
        val wither:  BossShock,
        val elderGuardian: BossShock,
        val breeze:  BossShock
    )

    data class BossShock(
        val shockPercent: Double,
        val category:     String,
        val message:      String
    )

    data class NightCycleConfig(
        val enabled:  Boolean,
        val nightfall: CategoryShockConfig,
        val dawn:      CategoryShockConfig
    )

    data class MassActivityConfig(
        val enabled:      Boolean,
        val threshold:    Int,
        val shockPercent: Double,
        val category:     String,
        val message:      String
    )
}
