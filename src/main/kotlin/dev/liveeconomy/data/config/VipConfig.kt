package dev.liveeconomy.data.config

/**
 * Typed configuration for VIP permission bonuses.
 *
 * Permissions:
 *  - liveeconomy.vip               — all VIP perks
 *  - liveeconomy.vip.short         — short selling access
 *  - liveeconomy.vip.alerts.extra  — extra alert slots
 *  - liveeconomy.vip.tax.discount  — reduced trade tax
 *  - liveeconomy.vip.cooldown.bypass — no trade cooldown
 *  - liveeconomy.vip.prestige      — prestige access
 */
data class VipConfig(
    val taxDiscountFactor: Double, // 0.90 = 10% tax reduction
    val extraAlertSlots:   Int
) {
    companion object {
        val DEFAULT = VipConfig(
            taxDiscountFactor = 0.90,
            extraAlertSlots   = 5
        )
    }
}
