package dev.liveeconomy.data.model

import org.bukkit.Material

/**
 * Player trading roles — each grants a category-specific bonus or tax discount.
 * Players choose via /market role or the RoleGUI.
 */
enum class PlayerRole(
    val displayName:  String,
    val description:  String,
    val iconMaterial: Material
) {
    MINER  ("⛏ Miner",   "Bonus sell revenue on Gems & Metals",       Material.IRON_PICKAXE),
    TRADER ("📈 Trader",  "Reduced trade tax across all categories",    Material.GOLD_INGOT),
    FARMER ("🌾 Farmer",  "Bonus sell revenue on Farming items",        Material.WHEAT),
    CRAFTER("🔨 Crafter", "Crafting events double market impact",       Material.CRAFTING_TABLE),
    NONE   ("— None",     "No active role. Choose one via /market role", Material.BARRIER);

    companion object {
        fun fromId(id: String): PlayerRole =
            entries.firstOrNull { it.name.equals(id, ignoreCase = true) } ?: NONE
    }
}
