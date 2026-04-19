package dev.liveeconomy.api.storage

import dev.liveeconomy.data.model.Transaction
import java.util.UUID

/**
 * Persistence interface for trade transaction history.
 */
interface TransactionStore {
    fun append(tx: Transaction)
    fun getRecent(playerUuid: UUID, limit: Int): List<Transaction>
    fun getAll(playerUuid: UUID): List<Transaction>
}
