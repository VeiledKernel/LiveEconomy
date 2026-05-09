package dev.liveeconomy.platform.listener

import dev.liveeconomy.api.player.PortfolioService
import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.core.alert.AlertService
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Handles player join/quit lifecycle events.
 * Thin adapter only — pre-warms cache on join, no-op on quit (MenuManager handles GUI cleanup).
 */
class PlayerListener(
    private val wallet:    WalletService,
    private val portfolio: PortfolioService,
    private val alerts:    AlertService
) : Listener {

    @EventHandler(priority = EventPriority.LOW)
    fun onJoin(event: PlayerJoinEvent) {
        wallet.getBalance(event.player)
        portfolio.getHoldings(event.player.uniqueId)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) { /* MenuManager handles session cleanup on inventory close */ }
}
