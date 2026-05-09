package dev.liveeconomy.core.player

import dev.liveeconomy.api.storage.PortfolioStore
import dev.liveeconomy.data.config.PrestigeConfig
import java.util.UUID

/**
 * Manages the prestige system — levels, eligibility, and per-level bonuses.
 * No Bukkit dependency — operates on UUID only.
 */
class PrestigeService(
    private val store:  PortfolioStore,
    private val config: PrestigeConfig
) {
    fun getLevel(uuid: UUID): Int = store.getPrestigeLevel(uuid)

    fun isEligible(uuid: UUID): Boolean {
        if (!config.enabled) return false
        if (getLevel(uuid) >= config.maxLevel) return false
        return store.getPnl(uuid).toDouble() >= config.requiredPnl
    }

    fun prestige(uuid: UUID): Boolean {
        if (!isEligible(uuid)) return false
        store.setPrestigeLevel(uuid, getLevel(uuid) + 1)
        return true
    }

    fun attempt(uuid: UUID): PrestigeResult {
        val level = getLevel(uuid)
        if (level >= config.maxLevel) return PrestigeResult.MaxLevel
        val pnl = store.getPnl(uuid).toDouble()
        if (pnl < config.requiredPnl) return PrestigeResult.NotEligible(
            requiredPnl          = config.requiredPnl,
            requiredPnlFormatted = "${"%.2f".format(config.requiredPnl)}"
        )
        store.setPrestigeLevel(uuid, level + 1)
        return PrestigeResult.Success(level + 1)
    }

    fun getSellBonus(uuid: UUID): Double =
        1.0 + (getLevel(uuid) * config.tradeBonusPercent / 100.0)

    fun getTaxReduction(uuid: UUID): Double =
        1.0 - (getLevel(uuid) * config.taxReductionPercent / 100.0).coerceAtMost(0.9)

    fun getAlertLimitBonus(uuid: UUID): Int =
        getLevel(uuid) * config.alertLimitBonus
}
