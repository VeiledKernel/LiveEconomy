package dev.liveeconomy.core.event.shock

import dev.liveeconomy.api.event.DomainEvent
import dev.liveeconomy.api.extension.ShockHandler
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.data.config.EventsConfig
import org.bukkit.event.block.BlockBreakEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Supply shock when a player mines threshold ores within a rolling window.
 * High mining activity = increased supply = price drop.
 */
class MiningShock(
    private val applier: ShockApplier,
    private val mapper:  ItemKeyMapper,
    private val config:  EventsConfig.ThresholdShockConfig
) : ShockHandler {

    private data class Counter(var count: Int = 0, var windowStart: Long = System.currentTimeMillis())
    private val counters = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Counter>>()

    override fun handle(event: DomainEvent) {
        // MiningShock is triggered by Bukkit BlockBreakEvent via ShockListener — not DomainEvent
        // This handle() is a no-op; the Bukkit listener calls onBlockBreak() directly
    }

    fun onBlockBreak(playerId: UUID, item: ItemKey) {
        if (!config.enabled) return
        val playerCounters = counters.getOrPut(playerId) { ConcurrentHashMap() }
        val counter        = playerCounters.getOrPut(item.id) { Counter() }
        val now            = System.currentTimeMillis()

        if (now - counter.windowStart > config.windowSeconds * 1000L) {
            counter.count       = 1
            counter.windowStart = now
        } else {
            counter.count++
        }

        if (counter.count >= config.threshold) {
            applier.applyToCategory(item.namespace, config.shockPercent, "mining", config.message)
            counter.count = 0
        }
    }
}
