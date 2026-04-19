package dev.liveeconomy.core.event.shock

import dev.liveeconomy.api.event.DomainEvent
import dev.liveeconomy.api.extension.ShockHandler
import dev.liveeconomy.data.config.EventsConfig

/** Supply shock on fish catch — treasure fishing boosts rare category. */
class FishingShock(
    private val applier: ShockApplier,
    private val config:  EventsConfig.FishingConfig
) : ShockHandler {
    override fun handle(event: DomainEvent) { }

    fun onFish(isTreasure: Boolean) {
        if (!config.enabled) return
        if (isTreasure) applier.applyToCategory("rare", config.treasureShock, "fishing", "Treasure Fishing — Rare Find!")
        else            applier.applyToCategory("food", config.regularShock,  "fishing", "Fishing Season — Food Supply Up")
    }
}
