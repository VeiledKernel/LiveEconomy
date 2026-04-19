package dev.liveeconomy.storage.yaml

import dev.liveeconomy.api.storage.TransactionScope

/**
 * No-op [TransactionScope] for YAML backends.
 *
 * **Known limitation — multi-store atomicity is not guaranteed.**
 * YAML cannot provide atomic cross-store transactions. Example failure scenario:
 * ```
 * walletStore.withdraw(uuid, cost)   // succeeds, written to wallets.yml
 * orderStore.addOrder(order)         // server crashes before this write
 * // Result: money deducted, order never placed → inconsistent state
 * ```
 * Each individual store write is crash-safe (via [AtomicYamlWriter]),
 * but there is no rollback mechanism across multiple stores.
 *
 * **This is a documented, accepted limitation of the YAML backend.**
 * It is consistent with the blueprint contract: YAML = best-effort,
 * SQL = ACID. Use [dev.liveeconomy.storage.sql.SqlTransactionScope]
 * with SQLite or MySQL for production deployments requiring atomicity.
 *
 * The `execute {}` wrapper exists to keep the interface consistent with
 * [dev.liveeconomy.storage.sql.SqlTransactionScope] so callers don't
 * need to know which backend they're using.
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
