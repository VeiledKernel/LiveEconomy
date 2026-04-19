package dev.liveeconomy.api.storage

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.data.model.PlayerStats
import dev.liveeconomy.data.model.ShortPosition
import java.math.BigDecimal
import java.util.UUID

/**
 * Persistence interface for player holdings, shorts, P&L, and stats.
 */
interface PortfolioStore {
    fun getHoldings(uuid: UUID): Map<ItemKey, Int>
    fun setHolding(uuid: UUID, item: ItemKey, quantity: Int)
    fun removeHolding(uuid: UUID, item: ItemKey)

    fun getPnl(uuid: UUID): BigDecimal
    fun addPnl(uuid: UUID, delta: BigDecimal)

    fun getStats(uuid: UUID): PlayerStats
    fun saveStats(uuid: UUID, stats: PlayerStats)

    fun getShortPositions(uuid: UUID): Map<ItemKey, ShortPosition>
    fun saveShortPosition(position: ShortPosition)
    fun removeShortPosition(uuid: UUID, item: ItemKey)
    fun getAllShortPositions(): Map<UUID, Map<ItemKey, ShortPosition>>

    fun getPrestigeLevel(uuid: UUID): Int
    fun setPrestigeLevel(uuid: UUID, level: Int)
}
