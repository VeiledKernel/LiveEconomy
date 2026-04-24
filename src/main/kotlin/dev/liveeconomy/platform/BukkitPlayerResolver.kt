package dev.liveeconomy.platform

import dev.liveeconomy.core.usecase.port.OnlinePlayerContext
import dev.liveeconomy.core.usecase.port.PlayerResolver
import org.bukkit.Bukkit
import java.util.UUID

/**
 * Bukkit implementation of [PlayerResolver].
 *
 * The ONLY class that calls [Bukkit.getPlayer] for domain resolution.
 * Must be called on the main thread.
 */
class BukkitPlayerResolver : PlayerResolver {

    override fun isOnline(uuid: UUID): Boolean =
        Bukkit.getPlayer(uuid) != null

    override fun withOnlinePlayer(uuid: UUID, action: OnlinePlayerContext.() -> Unit) {
        val player = Bukkit.getPlayer(uuid) ?: return
        action(object : OnlinePlayerContext {
            override val uuid: UUID get() = player.uniqueId
            override fun sendMessage(message: String) = player.sendMessage(message)
        })
    }
}
