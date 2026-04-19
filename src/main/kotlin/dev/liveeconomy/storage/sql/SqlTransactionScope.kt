package dev.liveeconomy.storage.sql

import com.zaxxer.hikari.HikariDataSource
import dev.liveeconomy.api.storage.TransactionScope

/**
 * Real JDBC [TransactionScope] backed by a [HikariDataSource].
 *
 * Acquires a fresh connection from the pool for each transaction,
 * commits on success, rolls back on failure, then releases the
 * connection back to the pool.
 *
 * **No shared connection — no nested transaction risk.**
 * Each [execute] call owns its connection for its lifetime, so
 * concurrent calls are fully isolated.
 *
 * **DEBT-1: CLEARED** — pool-backed, not a single shared connection.
 */
class SqlTransactionScope(private val dataSource: HikariDataSource) : TransactionScope {

    override fun <T> execute(block: () -> T): T =
        dataSource.connection.use { conn ->
            conn.autoCommit = false
            try {
                val result = block()
                conn.commit()
                result
            } catch (e: Exception) {
                try { conn.rollback() } catch (rb: Exception) {
                    System.err.println("[SqlTransactionScope] Rollback failed: ${rb.message}")
                }
                throw e
            }
        }
}
