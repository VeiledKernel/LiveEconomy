package dev.liveeconomy.storage.sql

import dev.liveeconomy.api.storage.TransactionScope
import java.sql.Connection

/**
 * Real JDBC [TransactionScope] for SQL backends.
 *
 * Wraps a [Connection] in a transaction — commits on success,
 * rolls back on any exception.
 *
 * **Nested transaction guard:** If called while already inside a transaction
 * (autoCommit is already false), the block is executed within the existing
 * transaction and no commit/rollback is issued. The outer transaction owner
 * retains full control. This avoids corrupting outer transaction state.
 *
 * **Phase 4 limitation:** This scope operates on a single shared [Connection].
 * Concurrent callers on different threads share the same connection, which
 * means concurrent transactions are not safe. This is acceptable for Phase 4
 * because trade execution is main-thread-bound. Phase 5 migration to
 * HikariCP [DataSource] will provide per-thread connection isolation.
 *
 * @see dev.liveeconomy.storage.yaml.YamlTransactionScope for the YAML no-op
 */
class SqlTransactionScope(private val connection: Connection) : TransactionScope {

    override fun <T> execute(block: () -> T): T {
        // Guard: if already inside a transaction, reuse it — do not nest
        if (!connection.autoCommit) {
            return block()
        }

        connection.autoCommit = false
        return try {
            val result = block()
            connection.commit()
            result
        } catch (e: Exception) {
            try { connection.rollback() } catch (rb: Exception) {
                System.err.println("[SqlTransactionScope] Rollback failed: ${rb.message}")
            }
            throw e
        } finally {
            connection.autoCommit = true
        }
    }
}
