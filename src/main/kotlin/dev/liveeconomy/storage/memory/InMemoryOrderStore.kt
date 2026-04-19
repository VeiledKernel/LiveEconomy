package dev.liveeconomy.storage.memory

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.storage.OrderStore
import dev.liveeconomy.data.model.TradeOrder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory [OrderStore] implementation for Phase 3.
 *
 * Orders are kept in a [ConcurrentHashMap] keyed by order ID.
 * Thread-safe for concurrent reads; writes are synchronized on [lock].
 *
 * **Phase 4 migration:** replace this with [dev.liveeconomy.storage.yaml.YamlOrderStore]
 * or [dev.liveeconomy.storage.sql.SqlOrderStore] in the composition root.
 * [dev.liveeconomy.core.economy.OrderBook] requires no changes — it depends
 * only on the [OrderStore] interface.
 *
 * **Limitation:** orders do not survive server restarts. This is a known,
 * documented limitation of the Phase 3 in-memory backing. Use a persistent
 * backend for production deployments.
 */
class InMemoryOrderStore : OrderStore {

    private val orders = ConcurrentHashMap<String, TradeOrder>()
    private val lock = Any()

    /**
     * No-op in Phase 3 — nothing to load from disk.
     * Phase 4 implementations override this to read from YAML/SQL.
     */
    override fun loadOpenOrders(): List<TradeOrder> = emptyList()

    override fun addOrder(order: TradeOrder) {
        synchronized(lock) {
            orders[order.orderId] = order
        }
    }

    override fun removeOrder(orderId: String) {
        synchronized(lock) {
            orders.remove(orderId)
        }
    }

    override fun updateOrder(order: TradeOrder) {
        synchronized(lock) {
            orders[order.orderId] = order
        }
    }

    override fun getOpenOrders(item: ItemKey): List<TradeOrder> {
        val active = orders.values.filter { it.item.id == item.id && !it.isExpired }
        val buys   = active.filter {  it.isBuyOrder }.sortedByDescending { it.targetPrice }
        val sells  = active.filter { !it.isBuyOrder }.sortedBy         { it.targetPrice }
        return buys + sells
    }

    override fun getPlayerOrders(playerUuid: UUID): List<TradeOrder> =
        orders.values
            .filter { it.playerUUID == playerUuid && !it.isExpired }
            .sortedByDescending { it.placedAt }

    override fun getAllOpenOrders(): List<TradeOrder> =
        orders.values
            .filter { !it.isExpired }
            .toList()
}
