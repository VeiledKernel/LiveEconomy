package dev.liveeconomy.storage.sql

import com.zaxxer.hikari.HikariDataSource
import dev.liveeconomy.api.storage.WalletStore
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * SQL-backed [WalletStore] using pool-based connection acquisition.
 *
 * In-memory cache reduces DB round-trips for hot reads.
 * Every write goes to DB immediately and updates the cache.
 */
class SqlWalletStore(
    private val ds:      HikariDataSource,
    private val dialect: SqlDialect
) : WalletStore {

    private val cache    = ConcurrentHashMap<UUID, Double>()
    private val upsertSql = dialect.upsert("wallets", "uuid", "balance")

    override fun getBalance(uuid: UUID): Double =
        cache.getOrPut(uuid) {
            ds.connection.use { conn ->
                conn.prepareStatement("SELECT balance FROM wallets WHERE uuid=?").use { ps ->
                    ps.setString(1, uuid.toString())
                    ps.executeQuery().use { rs -> if (rs.next()) rs.getDouble(1) else 0.0 }
                }
            }
        }

    override fun setBalance(uuid: UUID, balance: Double) {
        cache[uuid] = balance
        ds.connection.use { conn ->
            conn.prepareStatement(upsertSql).use { ps ->
                ps.setString(1, uuid.toString()); ps.setDouble(2, balance); ps.executeUpdate()
            }
        }
    }

    override fun getAllBalances(): Map<UUID, Double> {
        val result = mutableMapOf<UUID, Double>()
        ds.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT uuid, balance FROM wallets").use { rs ->
                    while (rs.next()) result[UUID.fromString(rs.getString(1))] = rs.getDouble(2)
                }
            }
        }
        return result
    }

    override fun saveAll(balances: Map<UUID, Double>) =
        balances.forEach { (uuid, balance) -> setBalance(uuid, balance) }
}
