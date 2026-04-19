package dev.liveeconomy.storage.sql

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.api.storage.OrderStore
import dev.liveeconomy.data.model.TradeOrder
import java.sql.Connection
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

/**
 * SQL-backed [OrderStore]. Full ACID guarantees via JDBC.
 */
class SqlOrderStore(
    private val conn:   Connection,
    private val mapper: ItemKeyMapper
) : OrderStore {

    override fun loadOpenOrders(): List<TradeOrder> {
        val orders = mutableListOf<TradeOrder>()
        conn.prepareStatement(
            "SELECT * FROM orders WHERE placed_at + (expiry_hours * 3600000) > ?"
        ).use { ps ->
            ps.setLong(1, System.currentTimeMillis())
            ps.executeQuery().use { rs ->
                while (rs.next()) orders.add(rs.toTradeOrder())
            }
        }
        return orders
    }

    override fun addOrder(order: TradeOrder) {
        conn.prepareStatement(
            "INSERT OR IGNORE INTO orders(order_id,player_uuid,player_name,item_id,quantity,target_price,is_buy_order,placed_at,expiry_hours) VALUES(?,?,?,?,?,?,?,?,?)"
        ).use { ps ->
            ps.setString(1, order.orderId)
            ps.setString(2, order.playerUUID.toString())
            ps.setString(3, order.playerName)
            ps.setString(4, order.item.id)
            ps.setInt(5, order.quantity)
            ps.setDouble(6, order.targetPrice)
            ps.setInt(7, if (order.isBuyOrder) 1 else 0)
            ps.setLong(8, order.placedAt.toEpochMilli())
            ps.setLong(9, order.expiryHours)
            ps.executeUpdate()
        }
    }

    override fun removeOrder(orderId: String) {
        conn.prepareStatement("DELETE FROM orders WHERE order_id = ?").use { ps ->
            ps.setString(1, orderId); ps.executeUpdate()
        }
    }

    override fun updateOrder(order: TradeOrder) {
        removeOrder(order.orderId)
        addOrder(order)
    }

    override fun getOpenOrders(item: ItemKey): List<TradeOrder> {
        val orders = mutableListOf<TradeOrder>()
        conn.prepareStatement(
            "SELECT * FROM orders WHERE item_id=? AND placed_at+(expiry_hours*3600000)>? ORDER BY target_price DESC"
        ).use { ps ->
            ps.setString(1, item.id); ps.setLong(2, System.currentTimeMillis())
            ps.executeQuery().use { rs -> while (rs.next()) orders.add(rs.toTradeOrder()) }
        }
        return orders
    }

    override fun getPlayerOrders(playerUuid: UUID): List<TradeOrder> {
        val orders = mutableListOf<TradeOrder>()
        conn.prepareStatement(
            "SELECT * FROM orders WHERE player_uuid=? AND placed_at+(expiry_hours*3600000)>? ORDER BY placed_at DESC"
        ).use { ps ->
            ps.setString(1, playerUuid.toString()); ps.setLong(2, System.currentTimeMillis())
            ps.executeQuery().use { rs -> while (rs.next()) orders.add(rs.toTradeOrder()) }
        }
        return orders
    }

    override fun getAllOpenOrders(): List<TradeOrder> = loadOpenOrders()

    private fun ResultSet.toTradeOrder() = TradeOrder(
        orderId     = getString("order_id"),
        playerUUID  = UUID.fromString(getString("player_uuid")),
        playerName  = getString("player_name"),
        item        = mapper.fromString(getString("item_id")),
        quantity    = getInt("quantity"),
        targetPrice = getDouble("target_price"),
        isBuyOrder  = getInt("is_buy_order") == 1,
        placedAt    = Instant.ofEpochMilli(getLong("placed_at")),
        expiryHours = getLong("expiry_hours")
    )
}
