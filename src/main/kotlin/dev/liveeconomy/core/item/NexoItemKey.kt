package dev.liveeconomy.core.item

import dev.liveeconomy.api.item.ItemKey

/**
 * [ItemKey] implementation for Nexo custom items.
 *
 * **Equality contract (Rule 12):** Two [NexoItemKey] instances are equal if
 * and only if their [id] values are equal. [customDisplayName] is a
 * presentation concern — it does NOT participate in equality or hashing.
 *
 * Not a `data class` because `customDisplayName` must be excluded from
 * generated `equals`/`hashCode`. Equality is implemented manually on [id].
 *
 * @param namespace always `"nexo"` for Nexo items
 * @param key the raw Nexo item ID — e.g. `"ruby"`, `"amethyst_sword"`
 * @param customDisplayName optional display name override. Does not affect equality.
 */
class NexoItemKey(
    override val namespace: String,
    override val key: String,
    val customDisplayName: String? = null
) : ItemKey {

    override val id: String = "$namespace:$key"

    override fun displayName(): String =
        customDisplayName
            ?: key.replace('_', ' ')
                .split(' ')
                .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }

    // ── Equality on id only — customDisplayName is excluded ──────────────────

    override fun equals(other: Any?): Boolean =
        other is ItemKey && id == other.id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "NexoItemKey($id)"
}
