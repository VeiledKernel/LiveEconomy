package dev.liveeconomy.core.event.shock

import dev.liveeconomy.api.event.DomainEvent
import dev.liveeconomy.api.extension.ShockHandler
import dev.liveeconomy.data.config.EventsConfig

/** Demand shock when online player count exceeds threshold — server rush hour. */
class MassActivityShock(
    private val applier: ShockApplier,
    private val config:  EventsConfig.MassActivityConfig
) : ShockHandler {
    override fun handle(event: DomainEvent) { }

    fun onPlayerCount(online: Int) {
        if (!config.enabled || online < config.threshold) return
        applier.applyToCategory(config.category, config.shockPercent, "mass_activity", config.message)
    }
}
