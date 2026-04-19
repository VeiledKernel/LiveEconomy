package dev.liveeconomy.core.player

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.player.PortfolioService
import dev.liveeconomy.api.storage.PortfolioStore
import dev.liveeconomy.data.model.PlayerStats
import dev.liveeconomy.data.model.ShortPosition
import dev.liveeconomy.data.model.Transaction
import java.math.BigDecimal
import java.util.UUID

/**
 * [PortfolioService] implementation.
 *
 * Pure delegation to [PortfolioStore] — no business logic here.
 * Business logic (e.g. prestige eligibility checks) lives in use cases.
 */
class PortfolioServiceImpl(
    private val store: PortfolioStore
) : PortfolioService {

    override fun getHoldings(playerUuid: UUID): Map<ItemKey, Int> =
        store.getHoldings(playerUuid)

    override fun getHolding(playerUuid: UUID, item: ItemKey): Int =
        store.getHoldings(playerUuid).getOrDefault(item, 0)

    override fun getTotalPnl(playerUuid: UUID): BigDecimal =
        store.getPnl(playerUuid)

    override fun getStats(playerUuid: UUID): PlayerStats =
        store.getStats(playerUuid)

    override fun getShortPositions(playerUuid: UUID): Map<ItemKey, ShortPosition> =
        store.getShortPositions(playerUuid)

    override fun getRecentTransactions(playerUuid: UUID, limit: Int): List<Transaction> =
        emptyList() // Delegated to TransactionStore in use cases — see TransactionService

    override fun getPrestigeLevel(playerUuid: UUID): Int =
        store.getPrestigeLevel(playerUuid)
}
