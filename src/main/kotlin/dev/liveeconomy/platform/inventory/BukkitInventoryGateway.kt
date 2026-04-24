package dev.liveeconomy.platform.inventory

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.core.usecase.port.InventoryGateway
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.UUID

/**
 * Bukkit implementation of [InventoryGateway].
 *
 * The ONLY class in the codebase that touches player inventory directly.
 * Must be called on the main thread — Paper enforces this at runtime.
 *
 * [mapper] is used to resolve [ItemKey] → [Material] for vanilla items.
 * Custom items (Nexo) with no Material equivalent are silently no-op'd
 * for inventory ops — their quantity is tracked in [PortfolioStore] only.
 */
class BukkitInventoryGateway(
    private val mapper: ItemKeyMapper
) : InventoryGateway {

    override fun spaceFor(playerUuid: UUID, item: ItemKey): Int {
        val player   = Bukkit.getPlayer(playerUuid) ?: return 0
        val material = mapper.toMaterial(item) ?: return Int.MAX_VALUE // custom items always fit
        var space    = 0
        for (stack in player.inventory.storageContents) {
            when {
                stack == null || stack.type == Material.AIR ->
                    space += material.maxStackSize
                stack.type == material ->
                    space += (material.maxStackSize - stack.amount).coerceAtLeast(0)
            }
        }
        return space
    }

    override fun countHeld(playerUuid: UUID, item: ItemKey): Int {
        val player   = Bukkit.getPlayer(playerUuid) ?: return 0
        val material = mapper.toMaterial(item) ?: return 0
        return player.inventory.contents
            .filterNotNull()
            .filter { it.type == material }
            .sumOf { it.amount }
    }

    override fun give(playerUuid: UUID, item: ItemKey, quantity: Int) {
        val player   = Bukkit.getPlayer(playerUuid) ?: return
        val material = mapper.toMaterial(item) ?: return  // Nexo items: portfolio-only
        player.inventory.addItem(ItemStack(material, quantity))
    }

    override fun take(playerUuid: UUID, item: ItemKey, quantity: Int) {
        val player   = Bukkit.getPlayer(playerUuid) ?: return
        val material = mapper.toMaterial(item) ?: return  // Nexo items: portfolio-only
        var remaining = quantity
        for (stack in player.inventory.contents.filterNotNull()) {
            if (stack.type == material && remaining > 0) {
                val remove = minOf(stack.amount, remaining)
                stack.amount -= remove
                remaining   -= remove
            }
        }
    }
}
