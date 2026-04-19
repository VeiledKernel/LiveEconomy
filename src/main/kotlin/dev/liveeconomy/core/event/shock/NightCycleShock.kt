package dev.liveeconomy.core.event.shock

import dev.liveeconomy.api.event.DomainEvent
import dev.liveeconomy.api.extension.ShockHandler
import dev.liveeconomy.data.config.EventsConfig

/** Shock fired at nightfall and dawn — affects mob drop prices. */
class NightCycleShock(
    private val applier: ShockApplier,
    private val config:  EventsConfig.NightCycleConfig
) : ShockHandler {
    override fun handle(event: DomainEvent) { }

    fun onNightfall() {
        if (!config.enabled) return
        applier.applyToCategory(config.nightfall.category, config.nightfall.shockPercent,
            "night_cycle", config.nightfall.message)
    }

    fun onDawn() {
        if (!config.enabled) return
        applier.applyToCategory(config.dawn.category, config.dawn.shockPercent,
            "night_cycle", config.dawn.message)
    }
}
