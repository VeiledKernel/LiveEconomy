package dev.liveeconomy.storage.yaml

import dev.liveeconomy.api.storage.TransactionScope

/**
 * No-op [TransactionScope] for YAML backends.
 *
 * YAML has no real transaction support. Operations execute sequentially
 * with no rollback on failure — partial writes are possible on crash.
 *
 * A warning is logged on first use to inform operators that they should
 * use a SQL backend for production deployments requiring ACID guarantees.
 *
 * **Contract:** per [TransactionScope] KDoc, YAML is explicitly documented
 * as best-effort only. This implementation fulfils that contract.
 */
class YamlTransactionScope : TransactionScope {

    @Volatile private var warned = false

    override fun <T> execute(block: () -> T): T {
        if (!warned) {
            System.err.println(
                "[YamlTransactionScope] WARNING: YAML storage has no real transaction " +
                "support. Partial writes are possible on crash. Use SQLite or MySQL " +
                "backend for production deployments."
            )
            warned = true
        }
        return block()
    }
}
