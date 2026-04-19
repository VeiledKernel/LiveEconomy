package dev.liveeconomy.api.item

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI

import org.bukkit.Material

/**
 * Centralised converter between [ItemKey] and Bukkit/plugin-specific item types.
 *
 * **This is the ONLY place in the codebase that performs Material ↔ ItemKey
 * conversion.** No code outside `core/item/` and `platform/` may call
 * `Material.matchMaterial()`, `material.key`, or equivalent directly when
 * an [ItemKey] is required.
 *
 * Supported namespaces:
 *  - `minecraft` — vanilla Bukkit [Material]
 *  - `nexo`      — Nexo custom items (when Nexo is installed)
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
interface ItemKeyMapper {

    /**
     * Convert a Bukkit [Material] to an [ItemKey].
     * Always succeeds — every Material has a valid `minecraft:*` identity.
     */
    fun fromMaterial(material: Material): ItemKey

    /**
     * Convert an [ItemKey] back to a Bukkit [Material], if one exists.
     *
     * Returns `null` for custom items (e.g. Nexo items) that have no
     * direct Material equivalent.
     */
    fun toMaterial(key: ItemKey): Material?

    /**
     * Convert a Nexo item ID string to an [ItemKey].
     *
     * @param nexoId the raw Nexo item ID (e.g. `"ruby"`)
     * @throws IllegalArgumentException if Nexo is not installed
     */
    fun fromNexoId(nexoId: String): ItemKey

    /**
     * Return true if this [ItemKey] represents a vanilla Minecraft item.
     */
    fun isVanilla(key: ItemKey): Boolean = key.namespace == "minecraft"

    /**
     * Return true if this [ItemKey] represents a Nexo custom item.
     */
    fun isNexo(key: ItemKey): Boolean = key.namespace == "nexo"
}
