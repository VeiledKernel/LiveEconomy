package dev.liveeconomy.api.player

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.data.model.PlayerStats
import dev.liveeconomy.data.model.ShortPosition
import dev.liveeconomy.data.model.Transaction
import java.math.BigDecimal
import java.util.UUID

/**
 * Player portfolio queries — holdings, P&L, stats, and trade history.
 *
 * All methods are read-only and thread-agnostic.
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
interface PortfolioService {

    /**
     * All items held by [playerUuid] and their quantities.
     * Returns an empty map if the player holds nothing.
     */
    fun getHoldings(playerUuid: UUID): Map<ItemKey, Int>

    /**
     * Quantity of [item] held by [playerUuid].
     * Returns 0 if none held.
     */
    fun getHolding(playerUuid: UUID, item: ItemKey): Int

    /**
     * Lifetime realised profit and loss for [playerUuid].
     * Positive = net profit, negative = net loss.
     */
    fun getTotalPnl(playerUuid: UUID): BigDecimal

    /**
     * Lifetime trading statistics for [playerUuid].
     */
    fun getStats(playerUuid: UUID): PlayerStats

    /**
     * All open short positions for [playerUuid].
     * Returns an empty map if the player has no open shorts.
     */
    fun getShortPositions(playerUuid: UUID): Map<ItemKey, ShortPosition>

    /**
     * Recent trade history for [playerUuid], newest first.
     *
     * @param limit maximum number of transactions to return
     */
    fun getRecentTransactions(playerUuid: UUID, limit: Int = 20): List<Transaction>

    /**
     * Prestige level for [playerUuid] (0 = no prestige).
     */
    fun getPrestigeLevel(playerUuid: UUID): Int

    /**
     * Total value of all holdings at current prices, pre-formatted for display.
     * Calculation performed by impl — callers (PAPI, GUI) receive a string only.
     */
    fun getPortfolioValueFormatted(playerUuid: UUID): String

    /**
     * Lifetime P&L with sign prefix, pre-formatted for display (+$1,234 / -$234).
     */
    fun getPnlFormatted(playerUuid: UUID): String

    /**
     * Player's current trader role. Read-only convenience for PAPI/view layer.
     */
    fun getRole(playerUuid: UUID): dev.liveeconomy.data.model.PlayerRole
}
