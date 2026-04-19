package dev.liveeconomy.storage.sql

import com.zaxxer.hikari.HikariDataSource
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.api.storage.TransactionStore
import dev.liveeconomy.data.model.TradeAction
import dev.liveeconomy.data.model.Transaction
import java.sql.ResultSet
import java.util.UUID

/** SQL-backed [TransactionStore] using pool-based connection acquisition. */
class SqlTransactionStore(
    private val ds:     HikariDataSource,
    private val mapper: ItemKeyMapper
) : TransactionStore {

    override fun append(tx: Transaction) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO transactions(player_uuid,timestamp,item_id,action,quantity,unit_price,total) VALUES(?,?,?,?,?,?,?)"
            ).use { ps ->
                ps.setString(1, tx.playerUuid.toString()); ps.setLong(2, tx.timestamp)
                ps.setString(3, tx.item.id); ps.setString(4, tx.action.name)
                ps.setInt(5, tx.quantity); ps.setDouble(6, tx.unitPrice); ps.setDouble(7, tx.total)
                ps.executeUpdate()
            }
        }
    }

    override fun getRecent(playerUuid: UUID, limit: Int): List<Transaction> {
        val result = mutableListOf<Transaction>()
        ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT * FROM transactions WHERE player_uuid=? ORDER BY timestamp DESC LIMIT ?"
            ).use { ps ->
                ps.setString(1, playerUuid.toString()); ps.setInt(2, limit)
                ps.executeQuery().use { rs ->
                    while (rs.next()) result.add(rs.toTransaction(playerUuid))
                }
            }
        }
        return result
    }

    override fun getAll(playerUuid: UUID): List<Transaction> {
        val result = mutableListOf<Transaction>()
        ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT * FROM transactions WHERE player_uuid=? ORDER BY timestamp DESC"
            ).use { ps ->
                ps.setString(1, playerUuid.toString())
                ps.executeQuery().use { rs ->
                    while (rs.next()) result.add(rs.toTransaction(playerUuid))
                }
            }
        }
        return result
    }

    private fun ResultSet.toTransaction(uuid: UUID) = Transaction(
        playerUuid = uuid,
        timestamp  = getLong("timestamp"),
        item       = mapper.fromString(getString("item_id")),
        action     = TradeAction.valueOf(getString("action")),
        quantity   = getInt("quantity"),
        unitPrice  = getDouble("unit_price"),
        total      = getDouble("total")
    )
}
