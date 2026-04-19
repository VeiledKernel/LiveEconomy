package dev.liveeconomy.core.item

import dev.liveeconomy.api.item.ItemKey

/**
 * [ItemKey] implementation for vanilla Minecraft items.
 *
 * Equality is based on [id] only (Rule 12 — data class ensures correct
 * equals/hashCode for use in maps and caches).
 *
 * @param namespace always `"minecraft"` for vanilla items
 * @param key the Bukkit material key — e.g. `"diamond"`, `"iron_ingot"`
 */
data class VanillaItemKey(
    override val namespace: String,
    override val key: String
) : ItemKey {

    // id is derived — not a constructor param, not part of equality.
    // equals/hashCode from data class cover namespace + key, which together
    // are equivalent to id equality for the "minecraft" namespace.
    override val id: String get() = "$namespace:$key"

    override fun displayName(): String =
        key.replace('_', ' ')
            .split(' ')
            .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }

    override fun toString(): String = id
}
