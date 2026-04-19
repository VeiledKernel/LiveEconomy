package dev.liveeconomy.core.event.shock

import dev.liveeconomy.api.event.DomainEvent
import dev.liveeconomy.api.extension.ShockHandler
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.data.config.EventsConfig
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Demand shock when a player bulk-crafts using a market ingredient.
 * High crafting demand = ingredient price rise.
 */
class CraftingShock(
    private val applier: ShockApplier,
    private val config:  EventsConfig.BulkCraftingConfig
) : ShockHandler {

    private data class Counter(var count: Int = 0, var windowStart: Long = System.currentTimeMillis())
    private val counters = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Counter>>()

    override fun handle(event: DomainEvent) { /* triggered via Bukkit listener */ }

    fun onCraft(playerId: UUID, ingredient: ItemKey) {
        if (!config.enabled) return
        val playerCounters = counters.getOrPut(playerId) { ConcurrentHashMap() }
        val counter        = playerCounters.getOrPut(ingredient.id) { Counter() }
        val now            = System.currentTimeMillis()

        if (now - counter.windowStart > config.windowSeconds * 1000L) {
            counter.count = 1; counter.windowStart = now
        } else counter.count++

        if (counter.count >= config.threshold) {
            applier.applyToCategory(ingredient.namespace, config.shockPercent, "crafting",
                "${config.messagePrefix} ${ingredient.displayName()}")
            counter.count = 0
        }
    }
}
