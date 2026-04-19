package dev.liveeconomy.core.usecase

import dev.liveeconomy.api.economy.TradeService
import dev.liveeconomy.api.economy.result.ShortResult
import dev.liveeconomy.api.item.ItemKey
import org.bukkit.entity.Player

/**
 * Orchestrates opening a short position.
 *
 * Single-service delegation in Phase 3 — will expand to coordinate
 * margin checks and analytics in a later phase.
 *
 * // No interface: use case, single concrete flow.
 */
class OpenShortUseCase(private val trade: TradeService) {
    fun execute(player: Player, item: ItemKey, quantity: Int): ShortResult =
        trade.openShort(player, item, quantity)
}
