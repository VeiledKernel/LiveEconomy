package dev.liveeconomy.storage.sql

import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.api.storage.TransactionStore
import dev.liveeconomy.data.model.TradeAction
import dev.liveeconomy.data.model.Transaction
import java.sql.Connection
import java.util.UUID

/**
 * SQL-backed [TransactionStore]. Append-only, indexed by player UUID.
 */
class SqlTransactionStore(
    private val conn:   Connection,
    private val mapper: ItemKeyMapper
) : TransactionStore {

    override fun append(tx: Transaction) {
        conn.prepareStatement(
            "INSERT INTO transactions(player_uuid,timestamp,item_id,action,quantity,unit_price,total) VALUES(?,?,?,?,?,?,?)"
        ).use { ps ->
            ps.setString(1, tx.playerUuid.toString())
            ps.setLong(2, tx.timestamp)
            ps.setString(3, tx.item.id)
            ps.setString(4, tx.action.name)
            ps.setInt(5, tx.quantity)
            ps.setDouble(6, tx.unitPrice)
            ps.setDouble(7, tx.total)
            ps.executeUpdate()
        }
    }

    override fun getRecent(playerUuid: UUID, limit: Int): List<Transaction> {
        val result = mutableListOf<Transaction>()
        conn.prepareStatement(
            "SELECT * FROM transactions WHERE player_uuid=? ORDER BY timestamp DESC LIMIT ?"
        ).use { ps ->
            ps.setString(1, playerUuid.toString()); ps.setInt(2, limit)
            ps.executeQuery().use { rs ->
                while (rs.next()) result.add(Transaction(
                    playerUuid = playerUuid,
                    timestamp  = rs.getLong("timestamp"),
                    item       = mapper.fromString(rs.getString("item_id")),
                    action     = TradeAction.valueOf(rs.getString("action")),
                    quantity   = rs.getInt("quantity"),
                    unitPrice  = rs.getDouble("unit_price"),
                    total      = rs.getDouble("total")
                ))
            }
        }
        return result
    }

    override fun getAll(playerUuid: UUID): List<Transaction> = getRecent(playerUuid, Int.MAX_VALUE)
}
