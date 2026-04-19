package dev.liveeconomy.core.event.shock

import dev.liveeconomy.api.event.DomainEvent
import dev.liveeconomy.api.extension.ShockHandler
import dev.liveeconomy.data.config.EventsConfig

/** Demand shock when a player enchants at high XP level — lapis demand rises. */
class EnchantingShock(
    private val applier: ShockApplier,
    private val config:  EventsConfig.EnchantingConfig
) : ShockHandler {
    override fun handle(event: DomainEvent) { }

    fun onEnchant(xpLevel: Int) {
        if (!config.enabled || xpLevel < config.minLevel) return
        applier.applyToCategory(config.category, config.shockPct, "enchanting", config.message)
    }
}
