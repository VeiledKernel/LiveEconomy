package dev.liveeconomy.util

import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * Inventory inspection utilities.
 *
 * All methods are pure reads — no inventory mutation here.
 * // No interface: stateless utility, never swapped
 */
object InventoryUtil {

    /**
     * Count how many items of [material] the player currently holds
     * across all inventory slots (excluding armour and off-hand).
     */
    fun countInInventory(player: Player, material: Material): Int =
        player.inventory.contents
            .filterNotNull()
            .filter { it.type == material }
            .sumOf { it.amount }

    /**
     * How many more items of [material] can fit into the player's inventory.
     * Counts empty slots (each holds maxStackSize) plus partial stacks.
     */
    fun spaceFor(player: Player, material: Material): Int {
        val stackSize = material.maxStackSize
        var space = 0
        for (stack in player.inventory.storageContents) {
            when {
                stack == null || stack.type == Material.AIR ->
                    space += stackSize
                stack.type == material ->
                    space += (stackSize - stack.amount).coerceAtLeast(0)
            }
        }
        return space
    }

    /**
     * Return true if the player's inventory has at least [amount] of [material].
     */
    fun hasAtLeast(player: Player, material: Material, amount: Int): Boolean =
        countInInventory(player, material) >= amount
}
