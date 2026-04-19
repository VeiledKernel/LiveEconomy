package dev.liveeconomy.core.event.shock

import dev.liveeconomy.api.event.DomainEvent
import dev.liveeconomy.api.extension.ShockHandler
import dev.liveeconomy.data.config.EventsConfig
import java.util.concurrent.atomic.AtomicInteger

/** Supply shock when multiple players die within a time window — mob drops flood. */
class DeathSpreeShock(
    private val applier: ShockApplier,
    private val config:  EventsConfig.ThresholdShockConfig
) : ShockHandler {
    private val count       = AtomicInteger(0)
    private var windowStart = System.currentTimeMillis()

    override fun handle(event: DomainEvent) { }

    fun onPlayerDeath() {
        if (!config.enabled) return
        val now = System.currentTimeMillis()
        if (now - windowStart > config.windowSeconds * 1000L) {
            count.set(1); windowStart = now
        } else if (count.incrementAndGet() >= config.threshold) {
            applier.applyToCategory(config.category, config.shockPercent, "death_spree", config.message)
            count.set(0)
        }
    }
}
