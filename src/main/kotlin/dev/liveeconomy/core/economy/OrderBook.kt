package dev.liveeconomy.core.economy

import dev.liveeconomy.api.economy.result.OrderResult
import dev.liveeconomy.core.economy.port.OrderBookPort
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.storage.OrderStore
import dev.liveeconomy.data.model.TradeOrder
import java.time.Instant
import java.util.UUID

/**
 * Manages the limit order book for all market items.
 *
 * Depends on [OrderStore] — not [PriceStore]. Order persistence and price
 * history are separate domain concerns (see design decision in OrderStore KDoc).
 *
 * Phase 3: backed by [dev.liveeconomy.storage.memory.InMemoryOrderStore].
 * Phase 4: composition root swaps in YAML/SQL implementation — no changes here.
 *
 * Internal use only — not part of the public api/ surface.
 * External plugins use [dev.liveeconomy.api.economy.TradeService.placeLimitOrder].
 */
class OrderBook(
    private val store:     OrderStore,
    private val maxOrders: Int = 10
) : OrderBookPort {

    /**
     * Load persisted orders from the store into memory.
     * Called once at startup after [YamlStorageProvider.start].
     * [InMemoryOrderStore.loadOpenOrders] returns empty list — no-op in Phase 3.
     * [YamlOrderStore.loadOpenOrders] restores all non-expired orders from disk.
     */
    fun init() {
        val persisted = store.loadOpenOrders()
        // Orders are already in the store's in-memory map after loadOpenOrders().
        // No additional action needed — getOpenOrders() reads from the store directly.
        if (persisted.isNotEmpty()) {
            System.out.println("[OrderBook] Restored ${persisted.size} open limit orders from storage.")
        }
    }

    // ── Place ─────────────────────────────────────────────────────────────────

    fun place(
        playerUuid:  UUID,
        playerName:  String,
        item:        ItemKey,
        quantity:    Int,
        targetPrice: Double,
        isBuyOrder:  Boolean,
        expiryHours: Long = 24L
    ): OrderResult {
        if (quantity <= 0 || targetPrice <= 0.0)
            return OrderResult.InvalidParameters

        val playerOrders = store.getPlayerOrders(playerUuid)
        if (playerOrders.size >= maxOrders)
            return OrderResult.LimitReached

        val order = TradeOrder(
            playerUUID  = playerUuid,
            playerName  = playerName,
            item        = item,
            quantity    = quantity,
            targetPrice = targetPrice,
            isBuyOrder  = isBuyOrder,
            placedAt    = Instant.now(),
            expiryHours = expiryHours
        )

        store.addOrder(order)
        return OrderResult.Placed(order.orderId)
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    fun cancel(playerUuid: UUID, orderId: String): OrderResult {
        val playerOrders = store.getPlayerOrders(playerUuid)
        val order = playerOrders.firstOrNull { it.orderId == orderId }
            ?: return OrderResult.NotFound

        store.removeOrder(order.orderId)
        return OrderResult.Cancelled
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    override fun getOpenOrders(item: ItemKey): List<TradeOrder> = store.getOpenOrders(item)
    fun getPlayerOrders(uuid: UUID): List<TradeOrder>     = store.getPlayerOrders(uuid)
    fun getAllOpenOrders(): List<TradeOrder>               = store.getAllOpenOrders()

    // ── Internal: process triggered orders ───────────────────────────────────

    /**
     * Returns orders that should trigger at [currentPrice] for [item].
     * Called by [TradeServiceImpl] during the market tick.
     * NOT exposed in the public API — internal orchestration only.
     */
    fun getTriggeredOrders(item: ItemKey, currentPrice: Double): List<TradeOrder> =
        store.getOpenOrders(item).filter { it.shouldTrigger(currentPrice) }

    /** Mark an order as filled — removes it from the store. */
    fun markFilled(orderId: String) = store.removeOrder(orderId)

    /** Remove all expired orders for [item]. Returns count removed. */
    fun pruneExpired(item: ItemKey): Int {
        val expired = store.getOpenOrders(item).filter { it.isExpired }
        expired.forEach { store.removeOrder(it.orderId) }
        return expired.size
    }
}
