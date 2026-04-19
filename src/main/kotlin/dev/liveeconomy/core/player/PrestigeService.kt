package dev.liveeconomy.core.player

import dev.liveeconomy.api.storage.PortfolioStore
import dev.liveeconomy.data.config.PrestigeConfig
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Manages the prestige system — levels, eligibility, and per-level bonuses.
 *
 * Prestige is earned by accumulating lifetime P&L above [PrestigeConfig.requiredPnl].
 * Each level stacks additional sell bonuses, tax reductions, and alert slots.
 */
class PrestigeService(
    private val store:  PortfolioStore,
    private val config: PrestigeConfig
) {
    fun getLevel(uuid: UUID): Int = store.getPrestigeLevel(uuid)

    fun isEligible(uuid: UUID): Boolean {
        if (!config.enabled) return false
        val level = getLevel(uuid)
        if (level >= config.maxLevel) return false
        return store.getPnl(uuid).toDouble() >= config.requiredPnl
    }

    fun prestige(player: Player): Boolean {
        if (!isEligible(player.uniqueId)) return false
        store.setPrestigeLevel(player.uniqueId, getLevel(player.uniqueId) + 1)
        return true
    }

    /** Additional sell revenue multiplier from prestige (stacks per level). */
    fun getSellBonus(uuid: UUID): Double =
        1.0 + (getLevel(uuid) * config.tradeBonusPercent / 100.0)

    /** Tax reduction multiplier from prestige (stacks per level). */
    fun getTaxReduction(uuid: UUID): Double =
        1.0 - (getLevel(uuid) * config.taxReductionPercent / 100.0).coerceAtMost(0.9)

    /** Extra alert slots granted by prestige level. */
    fun getAlertLimitBonus(uuid: UUID): Int =
        getLevel(uuid) * config.alertLimitBonus
}
