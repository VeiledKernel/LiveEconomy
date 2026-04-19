package dev.liveeconomy.core.event.shock

import dev.liveeconomy.api.event.DomainEvent
import dev.liveeconomy.api.extension.ShockHandler
import dev.liveeconomy.data.config.EventsConfig

/** Shock fired on raid victory or loss. */
class RaidShock(
    private val applier: ShockApplier,
    private val config:  EventsConfig.RaidConfig
) : ShockHandler {
    override fun handle(event: DomainEvent) { }

    fun onRaidVictory() {
        if (!config.enabled) return
        applier.applyToCategory("mob", -10.0, "raid_win", "Raid Victory — Pillager Loot Surplus")
        applier.applyToCategory("gems", 8.0, "raid_win", "Raid Victory — Emerald Trade Demand")
    }

    fun onRaidLoss() {
        if (!config.enabled) return
        applier.applyToCategory("food", 15.0, "raid_loss", "Village Sacked — Panic Buying!")
        applier.applyToCategory("wood", 12.0, "raid_loss", "Village Sacked — Rebuild Demand!")
    }
}
