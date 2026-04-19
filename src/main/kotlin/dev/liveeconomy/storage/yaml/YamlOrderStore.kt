package dev.liveeconomy.storage.yaml

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.api.storage.OrderStore
import dev.liveeconomy.core.item.BukkitItemKeyMapper
import dev.liveeconomy.data.model.TradeOrder
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * YAML-backed [OrderStore].
 *
 * Replaces [dev.liveeconomy.storage.memory.InMemoryOrderStore] in Phase 4.
 * [dev.liveeconomy.core.economy.OrderBook] requires no changes — interface is unchanged.
 *
 * File layout: `plugins/LiveEconomy/orders.yml`
 * ```yaml
 * orders:
 *   <orderId>:
 *     playerUuid: "..."
 *     playerName: "..."
 *     item: "minecraft:diamond"
 *     quantity: 10
 *     targetPrice: 250.0
 *     isBuyOrder: true
 *     placedAt: 1234567890000
 *     expiryHours: 24
 * ```
 *
 * Write strategy: every mutation writes the full file immediately.
 * This is safe for order volumes (< 1000 open orders typical) but will
 * be replaced by SQL in a later phase for high-volume servers.
 *
 * Thread safety: in-memory map uses [ConcurrentHashMap]; file writes are
 * synchronized on [fileLock]. All writes happen on the main thread
 * (called from [dev.liveeconomy.core.economy.OrderBook] via TradeService,
 * which is main-thread-bound).
 */
class YamlOrderStore(
    private val file:   File,
    private val mapper: BukkitItemKeyMapper
) : OrderStore {

    private val orders   = ConcurrentHashMap<String, TradeOrder>()
    private val fileLock = Any()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun loadOpenOrders(): List<TradeOrder> {
        if (!file.exists()) return emptyList()

        val yaml   = YamlConfiguration.loadConfiguration(file)
        val section = yaml.getConfigurationSection("orders") ?: return emptyList()

        for (orderId in section.getKeys(false)) {
            val sec = section.getConfigurationSection(orderId) ?: continue
            try {
                val itemKey = mapper.fromString(sec.getString("item") ?: continue)
                val order = TradeOrder(
                    orderId     = orderId,
                    playerUUID  = UUID.fromString(sec.getString("playerUuid") ?: continue),
                    playerName  = sec.getString("playerName") ?: "Unknown",
                    item        = itemKey,
                    quantity    = sec.getInt("quantity"),
                    targetPrice = sec.getDouble("targetPrice"),
                    isBuyOrder  = sec.getBoolean("isBuyOrder"),
                    placedAt    = Instant.ofEpochMilli(sec.getLong("placedAt")),
                    expiryHours = sec.getLong("expiryHours", 24L)
                )
                if (!order.isExpired) orders[orderId] = order
            } catch (e: Exception) {
                // Malformed entry — skip and continue loading the rest
                System.err.println("[YamlOrderStore] Skipping malformed order '$orderId': ${e.message}")
            }
        }

        return orders.values.toList()
    }

    // ── Mutations — write-through ─────────────────────────────────────────────

    override fun addOrder(order: TradeOrder) {
        orders[order.orderId] = order
        persist()
    }

    override fun removeOrder(orderId: String) {
        orders.remove(orderId)
        persist()
    }

    override fun updateOrder(order: TradeOrder) {
        orders[order.orderId] = order
        persist()
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    override fun getOpenOrders(item: ItemKey): List<TradeOrder> =
        orders.values
            .filter { it.item.id == item.id && !it.isExpired }
            .sortedByDescending { it.targetPrice }

    override fun getPlayerOrders(playerUuid: UUID): List<TradeOrder> =
        orders.values
            .filter { it.playerUUID == playerUuid && !it.isExpired }
            .sortedByDescending { it.placedAt }

    override fun getAllOpenOrders(): List<TradeOrder> =
        orders.values.filter { !it.isExpired }.toList()

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun persist() {
        synchronized(fileLock) {
            val yaml = YamlConfiguration()
            for ((id, order) in orders) {
                val path = "orders.$id"
                yaml.set("$path.playerUuid",  order.playerUUID.toString())
                yaml.set("$path.playerName",  order.playerName)
                yaml.set("$path.item",        order.item.id)
                yaml.set("$path.quantity",    order.quantity)
                yaml.set("$path.targetPrice", order.targetPrice)
                yaml.set("$path.isBuyOrder",  order.isBuyOrder)
                yaml.set("$path.placedAt",    order.placedAt.toEpochMilli())
                yaml.set("$path.expiryHours", order.expiryHours)
            }
            file.parentFile?.mkdirs()
            yaml.save(file)
        }
    }
}
