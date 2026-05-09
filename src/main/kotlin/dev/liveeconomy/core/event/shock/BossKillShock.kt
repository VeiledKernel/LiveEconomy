package dev.liveeconomy.core.event.shock

import dev.liveeconomy.api.event.DomainEvent
import dev.liveeconomy.api.extension.ShockHandler
import dev.liveeconomy.data.config.EventsConfig

/**
 * Shock fired when a boss mob is killed.
 *
 * Receives a neutral string entity type ID from
 * [dev.liveeconomy.platform.listener.ShockListener] — keeps core free of Bukkit.
 *
 * Supported boss IDs: "ENDER_DRAGON", "WITHER", "ELDER_GUARDIAN", "BREEZE"
 */
class BossKillShock(
    private val applier: ShockApplier,
    private val config:  EventsConfig.BossKillsConfig
) : ShockHandler {

    override fun handle(event: DomainEvent) { /* triggered via ShockListener */ }

    /** @param entityTypeId Bukkit EntityType.name(), e.g. "ENDER_DRAGON" */
    fun onBossKill(entityTypeId: String) {
        if (!config.enabled) return
        val shock = when (entityTypeId.uppercase()) {
            "ENDER_DRAGON"   -> config.dragon
            "WITHER"         -> config.wither
            "ELDER_GUARDIAN" -> config.elderGuardian
            "BREEZE"         -> config.breeze
            else             -> return
        }
        applier.applyToCategory(shock.category, shock.shockPercent, "boss_kill", shock.message)
    }
}
