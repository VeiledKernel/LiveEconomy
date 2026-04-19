package dev.liveeconomy.storage.sql

import dev.liveeconomy.api.storage.TransactionScope
import java.sql.Connection

/**
 * Real JDBC [TransactionScope] for SQL backends.
 *
 * Wraps a [Connection] in a transaction — commits on success,
 * rolls back on any exception. Used by SQL store implementations
 * to guarantee atomicity for multi-step writes.
 *
 * Contrast with [dev.liveeconomy.storage.yaml.YamlTransactionScope]
 * which is a no-op because YAML has no rollback capability.
 */
class SqlTransactionScope(private val connection: Connection) : TransactionScope {

    override fun <T> execute(block: () -> T): T {
        val wasAutoCommit = connection.autoCommit
        connection.autoCommit = false
        return try {
            val result = block()
            connection.commit()
            result
        } catch (e: Exception) {
            connection.rollback()
            throw e
        } finally {
            connection.autoCommit = wasAutoCommit
        }
    }
}
