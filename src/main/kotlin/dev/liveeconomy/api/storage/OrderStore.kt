package dev.liveeconomy.api.storage

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.data.model.TradeOrder
import java.util.UUID

/**
 * Persistence interface for pending limit orders.
 *
 * Limit orders represent player intent and pending financial state —
 * they are restart-sensitive and must not be treated as transient cache.
 *
 * Phase 3: backed by [dev.liveeconomy.storage.memory.InMemoryOrderStore].
 * Phase 4: replaced by YAML/SQL implementations with no changes to
 *          [dev.liveeconomy.core.economy.OrderBook] or its callers.
 *
 * Separated from [PriceStore] intentionally — price data and order book
 * state are different domain concerns with different write patterns.
 */
interface OrderStore {

    /** Load all open orders — called once at startup. */
    fun loadOpenOrders(): List<TradeOrder>

    /** Persist a newly placed order. */
    fun addOrder(order: TradeOrder)

    /** Remove an order by its [orderId] (filled or cancelled). */
    fun removeOrder(orderId: String)

    /** Replace an existing order (e.g. partial fill update). */
    fun updateOrder(order: TradeOrder)

    /** All open orders for [item], sorted by target price descending. */
    fun getOpenOrders(item: ItemKey): List<TradeOrder>

    /** All open orders placed by [playerUuid]. */
    fun getPlayerOrders(playerUuid: UUID): List<TradeOrder>

    /** All open orders across all items and players. */
    fun getAllOpenOrders(): List<TradeOrder>
}
