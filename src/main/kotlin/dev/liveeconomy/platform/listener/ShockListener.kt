package dev.liveeconomy.platform.listener

import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.core.event.shock.*
import dev.liveeconomy.data.config.EventsConfig
import org.bukkit.block.data.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.raid.RaidFinishEvent

/**
 * Routes Bukkit gameplay events to domain shock handlers.
 * Thin adapter only — no price mutation, no storage access, no business logic.
 * All Bukkit event parsing lives here. Core shock classes receive domain-neutral data.
 */
class ShockListener(
    private val mining:     MiningShock,
    private val harvest:    HarvestShock,
    private val fishing:    FishingShock,
    private val enchanting: EnchantingShock,
    private val bossKill:   BossKillShock,
    private val raid:       RaidShock,
    private val mapper:     ItemKeyMapper,
    private val config:     EventsConfig
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!config.enabled) return
        val itemKey = runCatching { mapper.fromMaterial(event.block.type) }.getOrNull() ?: return
        mining.onBlockBreak(event.player.uniqueId, itemKey)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onHarvest(event: BlockBreakEvent) {
        if (!config.enabled) return
        val data = event.block.blockData as? Ageable ?: return
        if (data.age < data.maximumAge) return
        harvest.onHarvest(event.player.uniqueId)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onFishing(event: PlayerFishEvent) {
        if (!config.enabled) return
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) return
        fishing.onFish(false)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDeath(event: EntityDeathEvent) {
        if (!config.enabled) return
        event.entity.killer ?: return
        // Pass neutral string ID — BossKillShock has zero Bukkit imports
        bossKill.onBossKill(event.entity.type.name)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEnchant(event: EnchantItemEvent) {
        if (!config.enabled) return
        enchanting.onEnchant(event.expLevelCost)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onRaid(event: RaidFinishEvent) {
        if (!config.enabled) return
        if (event.winners.isNotEmpty()) raid.onRaidVictory()
    }
}
