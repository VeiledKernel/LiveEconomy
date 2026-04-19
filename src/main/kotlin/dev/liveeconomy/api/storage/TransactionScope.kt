package dev.liveeconomy.api.storage

/**
 * Wraps multi-step write operations in an atomic boundary.
 *
 * Behaviour by backend:
 * - MySQL/SQLite: real JDBC transaction — full rollback on failure
 * - YAML: best-effort sequential writes — no rollback on crash
 *
 * Usage:
 * ```kotlin
 * transactionScope.execute {
 *     walletStore.setBalance(uuid, newBalance)
 *     portfolioStore.setHolding(uuid, item, newQty)
 *     transactionStore.append(tx)
 * }
 * ```
 */
interface TransactionScope {
    fun <T> execute(block: () -> T): T
}
