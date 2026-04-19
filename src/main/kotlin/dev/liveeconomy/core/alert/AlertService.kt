package dev.liveeconomy.core.alert

import dev.liveeconomy.api.event.DomainEvent
import dev.liveeconomy.api.event.DomainEventHandler
import dev.liveeconomy.api.event.PriceChangedEvent
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.scheduler.Scheduler
import dev.liveeconomy.data.config.MarketConfig
import dev.liveeconomy.data.config.VipConfig
import dev.liveeconomy.data.model.Alert
import dev.liveeconomy.data.model.Direction
import dev.liveeconomy.util.SoundUtil
import dev.liveeconomy.core.usecase.port.PlayerResolver
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages price alerts — stores alerts and fires them when prices cross thresholds.
 *
 * Subscribes to [PriceChangedEvent] via the [DomainEventBus] — zero coupling
 * to the market engine. The bus calls [handle] synchronously on each price change.
 *
 * Heavy work (sendMessage, sound) is dispatched to the main thread via [Scheduler].
 */
class AlertService(
    private val config:    MarketConfig,
    private val vipConfig: VipConfig,
    private val scheduler: Scheduler,
    private val playerResolver: PlayerResolver
) : DomainEventHandler {

    private val alerts = ConcurrentHashMap<UUID, MutableList<Alert>>()

    // ── DomainEventHandler ────────────────────────────────────────────────────

    override fun handle(event: DomainEvent) {
        if (event !is PriceChangedEvent) return
        checkAlerts(event.item, event.newPrice)
    }

    // ── Alert management ──────────────────────────────────────────────────────

    fun addAlert(player: Player, item: ItemKey, targetPrice: Double, direction: Direction): Boolean {
        val limit = getAlertLimit(player)
        val list  = alerts.getOrPut(player.uniqueId) { mutableListOf() }
        if (list.size >= limit) return false
        list += Alert(player.uniqueId, item, targetPrice, direction)
        return true
    }

    fun removeAlert(playerUuid: UUID, item: ItemKey): Boolean {
        val list = alerts[playerUuid] ?: return false
        return list.removeIf { it.item.id == item.id }
    }

    fun getAlerts(playerUuid: UUID): List<Alert> =
        alerts[playerUuid]?.toList() ?: emptyList()

    fun getAlertLimit(player: Player): Int {
        var limit = config.alertMaxPerPlayer
        if (player.hasPermission("liveeconomy.vip.alerts.extra")) limit += vipConfig.extraAlertSlots
        return limit
    }

    fun loadAll(saved: Map<UUID, List<Alert>>) {
        alerts.clear()
        saved.forEach { (uuid, list) -> alerts[uuid] = list.toMutableList() }
    }

    fun getAll(): Map<UUID, List<Alert>> = alerts.mapValues { it.value.toList() }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun checkAlerts(item: ItemKey, currentPrice: Double) {
        val fired = mutableListOf<Pair<UUID, Alert>>()

        for ((uuid, list) in alerts) {
            for (alert in list) {
                if (alert.item.id != item.id) continue
                if (!alert.isTriggered(currentPrice)) continue
                fired += uuid to alert
            }
        }

        for ((uuid, alert) in fired) {
            alerts[uuid]?.remove(alert)
            val dir = if (alert.direction == Direction.ABOVE) "§aabove" else "§cbelow"
            val msg = "§8[§6§lMarket§8] §r§e🔔 §f${alert.item.displayName()} " +
                "is now $dir §6\$${String.format("%.2f", alert.targetPrice)} " +
                "§8— §7now \$${String.format("%.2f", currentPrice)}"
            scheduler.runOnMain {
                playerResolver.withOnlinePlayer(uuid) { sendMessage(msg) }
            }
        }
    }
}
