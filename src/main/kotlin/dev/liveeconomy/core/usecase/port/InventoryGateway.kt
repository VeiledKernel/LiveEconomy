package dev.liveeconomy.core.usecase.port

import dev.liveeconomy.api.item.ItemKey
import java.util.UUID

/**
 * Abstracts player inventory operations for use in [dev.liveeconomy.core.usecase].
 *
 * Keeps `core/` free of Bukkit imports and main-thread coupling.
 * The platform-side implementation lives in
 * [dev.liveeconomy.platform.inventory.BukkitInventoryGateway] and MUST
 * be called on the main thread.
 *
 * // Internal interface — not part of public api/
 */
internal interface InventoryGateway {

    /**
     * How many more items of [item] can fit in the player's inventory.
     * Returns 0 if inventory is full.
     */
    fun spaceFor(playerUuid: UUID, item: ItemKey): Int

    /**
     * How many items of [item] the player currently holds.
     */
    fun countHeld(playerUuid: UUID, item: ItemKey): Int

    /**
     * Add [quantity] units of [item] to the player's inventory.
     * Caller must ensure [spaceFor] >= quantity before calling.
     */
    fun give(playerUuid: UUID, item: ItemKey, quantity: Int)

    /**
     * Remove [quantity] units of [item] from the player's inventory.
     * Caller must ensure [countHeld] >= quantity before calling.
     */
    fun take(playerUuid: UUID, item: ItemKey, quantity: Int)
}
