package dev.liveeconomy.core.event.shock

import dev.liveeconomy.api.event.DomainEvent
import dev.liveeconomy.api.extension.ShockHandler
import dev.liveeconomy.data.config.EventsConfig
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Supply shock when a player harvests threshold crops within a rolling window.
 * Large harvests = food surplus = food price drop.
 */
class HarvestShock(
    private val applier: ShockApplier,
    private val config:  EventsConfig.CategoryShockConfig
) : ShockHandler {

    private data class Counter(var count: Int = 0, var windowStart: Long = System.currentTimeMillis())
    private val counters = ConcurrentHashMap<UUID, Counter>()

    override fun handle(event: DomainEvent) { /* triggered via Bukkit listener */ }

    fun onHarvest(playerId: UUID) {
        if (!config.enabled) return
        val counter = counters.getOrPut(playerId) { Counter() }
        val now     = System.currentTimeMillis()

        if (now - counter.windowStart > config.windowSeconds * 1000L) {
            counter.count = 1; counter.windowStart = now
        } else counter.count++

        if (counter.count >= config.threshold) {
            applier.applyToCategory(config.category, config.shockPercent, "harvest", config.message)
            counter.count = 0
        }
    }
}
