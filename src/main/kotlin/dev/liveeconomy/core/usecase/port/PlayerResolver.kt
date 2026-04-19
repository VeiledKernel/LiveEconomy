package dev.liveeconomy.core.usecase.port

import java.util.UUID

/**
 * Abstracts online player lookup out of [dev.liveeconomy.core].
 *
 * Prevents core/ from importing [org.bukkit.Bukkit] directly.
 * Platform implementation: [dev.liveeconomy.platform.BukkitPlayerResolver].
 *
 * // Internal interface — not part of public api/
 */
internal interface PlayerResolver {

    /**
     * Returns true if the player with [uuid] is currently online.
     */
    fun isOnline(uuid: UUID): Boolean

    /**
     * Calls [action] with the online player context if the player is online.
     * No-op if the player is offline.
     *
     * Must be called on the main thread — [action] touches Bukkit API.
     */
    fun withOnlinePlayer(uuid: UUID, action: OnlinePlayerContext.() -> Unit)
}

/**
 * Scoped context for operations on a confirmed-online player.
 * Implementations carry the Bukkit Player reference internally.
 */
interface OnlinePlayerContext {
    val uuid: UUID
    fun sendMessage(message: String)
}
