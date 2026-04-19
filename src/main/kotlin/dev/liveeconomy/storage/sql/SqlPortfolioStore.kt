package dev.liveeconomy.storage.sql

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.api.storage.PortfolioStore
import dev.liveeconomy.data.model.PlayerStats
import dev.liveeconomy.data.model.ShortPosition
import java.math.BigDecimal
import java.sql.Connection
import java.util.UUID

/**
 * SQL-backed [PortfolioStore]. Full ACID via JDBC.
 */
class SqlPortfolioStore(
    private val conn:   Connection,
    private val mapper: ItemKeyMapper
) : PortfolioStore {

    override fun getHoldings(uuid: UUID): Map<ItemKey, Int> {
        val result = mutableMapOf<ItemKey, Int>()
        conn.prepareStatement("SELECT item_id, quantity FROM holdings WHERE uuid=?").use { ps ->
            ps.setString(1, uuid.toString())
            ps.executeQuery().use { rs ->
                while (rs.next()) result[mapper.fromString(rs.getString(1))] = rs.getInt(2)
            }
        }
        return result
    }

    override fun setHolding(uuid: UUID, item: ItemKey, quantity: Int) {
        conn.prepareStatement(
            "INSERT INTO holdings(uuid,item_id,quantity) VALUES(?,?,?) ON CONFLICT(uuid,item_id) DO UPDATE SET quantity=excluded.quantity"
        ).use { ps -> ps.setString(1, uuid.toString()); ps.setString(2, item.id); ps.setInt(3, quantity); ps.executeUpdate() }
    }

    override fun removeHolding(uuid: UUID, item: ItemKey) {
        conn.prepareStatement("DELETE FROM holdings WHERE uuid=? AND item_id=?").use { ps ->
            ps.setString(1, uuid.toString()); ps.setString(2, item.id); ps.executeUpdate()
        }
    }

    override fun getPnl(uuid: UUID): BigDecimal =
        conn.prepareStatement("SELECT amount FROM pnl WHERE uuid=?").use { ps ->
            ps.setString(1, uuid.toString())
            ps.executeQuery().use { rs -> if (rs.next()) BigDecimal.valueOf(rs.getDouble(1)) else BigDecimal.ZERO }
        }

    override fun addPnl(uuid: UUID, delta: BigDecimal) {
        conn.prepareStatement(
            "INSERT INTO pnl(uuid,amount) VALUES(?,?) ON CONFLICT(uuid) DO UPDATE SET amount=amount+excluded.amount"
        ).use { ps -> ps.setString(1, uuid.toString()); ps.setDouble(2, delta.toDouble()); ps.executeUpdate() }
    }

    override fun getStats(uuid: UUID): PlayerStats =
        conn.prepareStatement("SELECT * FROM player_stats WHERE uuid=?").use { ps ->
            ps.setString(1, uuid.toString())
            ps.executeQuery().use { rs ->
                if (rs.next()) PlayerStats(
                    totalBuys = rs.getInt("total_buys"), totalSells = rs.getInt("total_sells"),
                    wins = rs.getInt("wins"), totalVolume = rs.getDouble("total_volume"),
                    totalRoi = rs.getDouble("total_roi")
                ) else PlayerStats()
            }
        }

    override fun saveStats(uuid: UUID, stats: PlayerStats) {
        conn.prepareStatement(
            "INSERT INTO player_stats(uuid,total_buys,total_sells,wins,total_volume,total_roi) VALUES(?,?,?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET total_buys=excluded.total_buys,total_sells=excluded.total_sells,wins=excluded.wins,total_volume=excluded.total_volume,total_roi=excluded.total_roi"
        ).use { ps ->
            ps.setString(1, uuid.toString()); ps.setInt(2, stats.totalBuys); ps.setInt(3, stats.totalSells)
            ps.setInt(4, stats.wins); ps.setDouble(5, stats.totalVolume); ps.setDouble(6, stats.totalRoi)
            ps.executeUpdate()
        }
    }

    override fun getShortPositions(uuid: UUID): Map<ItemKey, ShortPosition> {
        val result = mutableMapOf<ItemKey, ShortPosition>()
        conn.prepareStatement("SELECT * FROM short_positions WHERE uuid=?").use { ps ->
            ps.setString(1, uuid.toString())
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    val item = mapper.fromString(rs.getString("item_id"))
                    result[item] = ShortPosition(uuid, item, rs.getInt("quantity"), rs.getDouble("entry_price"), rs.getDouble("collateral"))
                }
            }
        }
        return result
    }

    override fun saveShortPosition(position: ShortPosition) {
        conn.prepareStatement(
            "INSERT INTO short_positions(uuid,item_id,quantity,entry_price,collateral) VALUES(?,?,?,?,?) ON CONFLICT(uuid,item_id) DO UPDATE SET quantity=excluded.quantity,entry_price=excluded.entry_price,collateral=excluded.collateral"
        ).use { ps ->
            ps.setString(1, position.playerUuid.toString()); ps.setString(2, position.item.id)
            ps.setInt(3, position.quantity); ps.setDouble(4, position.entryPrice); ps.setDouble(5, position.collateral)
            ps.executeUpdate()
        }
    }

    override fun removeShortPosition(uuid: UUID, item: ItemKey) {
        conn.prepareStatement("DELETE FROM short_positions WHERE uuid=? AND item_id=?").use { ps ->
            ps.setString(1, uuid.toString()); ps.setString(2, item.id); ps.executeUpdate()
        }
    }

    override fun getAllShortPositions(): Map<UUID, Map<ItemKey, ShortPosition>> {
        val result = mutableMapOf<UUID, MutableMap<ItemKey, ShortPosition>>()
        conn.createStatement().use { stmt ->
            stmt.executeQuery("SELECT * FROM short_positions").use { rs ->
                while (rs.next()) {
                    val uuid = UUID.fromString(rs.getString("uuid"))
                    val item = mapper.fromString(rs.getString("item_id"))
                    val pos  = ShortPosition(uuid, item, rs.getInt("quantity"), rs.getDouble("entry_price"), rs.getDouble("collateral"))
                    result.getOrPut(uuid) { mutableMapOf() }[item] = pos
                }
            }
        }
        return result
    }

    override fun getPrestigeLevel(uuid: UUID): Int =
        conn.prepareStatement("SELECT level FROM prestige WHERE uuid=?").use { ps ->
            ps.setString(1, uuid.toString())
            ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }

    override fun setPrestigeLevel(uuid: UUID, level: Int) {
        conn.prepareStatement(
            "INSERT INTO prestige(uuid,level) VALUES(?,?) ON CONFLICT(uuid) DO UPDATE SET level=excluded.level"
        ).use { ps -> ps.setString(1, uuid.toString()); ps.setInt(2, level); ps.executeUpdate() }
    }
}
