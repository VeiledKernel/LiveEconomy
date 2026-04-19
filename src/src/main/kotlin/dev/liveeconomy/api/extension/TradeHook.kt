package dev.liveeconomy.api.extension

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI
import dev.liveeconomy.api.item.ItemKey
import org.bukkit.entity.Player

/**
 * Extension point for trade lifecycle hooks.
 *
 * Allows external plugins to observe or modify trade behaviour
 * without modifying core trade logic.
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
interface TradeHook {

    /**
     * Called before a trade executes.
     * Return false to cancel the trade entirely.
     */
    fun beforeTrade(player: Player, item: ItemKey, quantity: Int, isBuy: Boolean): Boolean = true

    /**
     * Called after a trade completes successfully.
     */
    fun afterTrade(player: Player, item: ItemKey, quantity: Int, isBuy: Boolean, total: Double) {}
}
