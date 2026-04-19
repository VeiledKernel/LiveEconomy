package dev.liveeconomy.data.config

/**
 * Typed configuration for the player role system.
 *
 * Separated from [MarketConfig] — roles are a player progression concern,
 * not a market engine concern.
 */
data class RolesConfig(
    val enabled:                Boolean,
    val roleChangeCooldownMs:   Long,    // hours converted to ms by ConfigLoader
    val roleTaxDiscount:        Double,  // TRADER: tax multiplier (0.5 = 50% reduction)
    val roleMinerBonus:         Double,  // MINER: sell revenue bonus for gems/metals
    val roleFarmerBonus:        Double,  // FARMER: sell revenue bonus for farm category
    val crafterShockMultiplier: Double   // CRAFTER: crafting shock strength multiplier
) {
    companion object {
        val DEFAULT = RolesConfig(
            enabled                = true,
            roleChangeCooldownMs   = 24 * 3_600_000L, // 24 hours
            roleTaxDiscount        = 0.50,
            roleMinerBonus         = 0.10,
            roleFarmerBonus        = 0.15,
            crafterShockMultiplier = 2.0
        )
    }
}
