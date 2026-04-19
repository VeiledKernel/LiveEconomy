package dev.liveeconomy.core.event.shock

import dev.liveeconomy.api.event.DomainEvent
import dev.liveeconomy.api.extension.ShockHandler
import dev.liveeconomy.data.config.EventsConfig
import org.bukkit.entity.EntityType

/**
 * Shock fired when a boss mob is killed.
 * Each boss type affects a different market category.
 */
class BossKillShock(
    private val applier: ShockApplier,
    private val config:  EventsConfig.BossKillsConfig
) : ShockHandler {

    override fun handle(event: DomainEvent) { /* triggered via Bukkit EntityDeathEvent in ShockListener */ }

    fun onBossKill(entityType: EntityType) {
        if (!config.enabled) return
        val shock = when (entityType) {
            EntityType.ENDER_DRAGON   -> config.dragon
            EntityType.WITHER         -> config.wither
            EntityType.ELDER_GUARDIAN -> config.elderGuardian
            EntityType.BREEZE         -> config.breeze
            else                      -> return
        }
        applier.applyToCategory(shock.category, shock.shockPercent, "boss_kill", shock.message)
    }
}
