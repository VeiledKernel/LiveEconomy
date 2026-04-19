package dev.liveeconomy.core.item

import dev.liveeconomy.api.item.ItemKey

/**
 * [ItemKey] implementation for vanilla Minecraft items.
 *
 * **Equality contract (Rule 12):** Two [VanillaItemKey] instances are equal
 * if and only if their [id] values are equal. Equality is implemented
 * manually on [id] for consistency with [NexoItemKey] — both use the same
 * contract regardless of implementation class.
 *
 * Not a `data class` to keep equality behaviour consistent with
 * [NexoItemKey] and to allow cross-class equality between different
 * [ItemKey] implementations that share the same [id].
 *
 * @param namespace always `"minecraft"` for vanilla items
 * @param key the Bukkit material key — e.g. `"diamond"`, `"iron_ingot"`
 */
class VanillaItemKey(
    override val namespace: String,
    override val key: String
) : ItemKey {

    override val id: String = "$namespace:$key"

    override fun displayName(): String =
        key.replace('_', ' ')
            .split(' ')
            .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }

    // ── Equality on id only — works across ItemKey implementations ────────────

    override fun equals(other: Any?): Boolean =
        other is ItemKey && id == other.id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "VanillaItemKey($id)"
}
