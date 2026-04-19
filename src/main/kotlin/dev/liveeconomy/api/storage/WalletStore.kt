package dev.liveeconomy.api.storage

import java.util.UUID

/**
 * Persistence interface for player wallet balances.
 * Narrow — balance operations only, no business logic.
 */
interface WalletStore {
    fun getBalance(uuid: UUID): Double
    fun setBalance(uuid: UUID, balance: Double)
    fun getAllBalances(): Map<UUID, Double>
    fun saveAll(balances: Map<UUID, Double>)
}
