package dev.liveeconomy.storage.sql

import dev.liveeconomy.api.storage.WalletStore
import java.sql.Connection
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * SQL-backed [WalletStore].
 *
 * Uses an in-memory read cache — balances are written through to DB on every
 * [setBalance] call. Cache is populated lazily on first [getBalance].
 */
class SqlWalletStore(private val conn: Connection) : WalletStore {

    private val cache = ConcurrentHashMap<UUID, Double>()

    override fun getBalance(uuid: UUID): Double =
        cache.getOrPut(uuid) {
            conn.prepareStatement("SELECT balance FROM wallets WHERE uuid = ?").use { ps ->
                ps.setString(1, uuid.toString())
                ps.executeQuery().use { rs -> if (rs.next()) rs.getDouble(1) else 0.0 }
            }
        }

    override fun setBalance(uuid: UUID, balance: Double) {
        cache[uuid] = balance
        conn.prepareStatement(
            "INSERT INTO wallets(uuid, balance) VALUES(?,?) ON CONFLICT(uuid) DO UPDATE SET balance=excluded.balance"
        ).use { ps -> ps.setString(1, uuid.toString()); ps.setDouble(2, balance); ps.executeUpdate() }
    }

    override fun getAllBalances(): Map<UUID, Double> {
        val result = mutableMapOf<UUID, Double>()
        conn.createStatement().use { stmt ->
            stmt.executeQuery("SELECT uuid, balance FROM wallets").use { rs ->
                while (rs.next()) result[UUID.fromString(rs.getString(1))] = rs.getDouble(2)
            }
        }
        return result
    }

    override fun saveAll(balances: Map<UUID, Double>) =
        balances.forEach { (uuid, balance) -> setBalance(uuid, balance) }
}
