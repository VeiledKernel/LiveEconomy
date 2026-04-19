package dev.liveeconomy.core.item

import dev.liveeconomy.api.item.ItemKey

/**
 * [ItemKey] implementation for Nexo custom items.
 *
 * Nexo items have no vanilla Material equivalent. [displayName] falls back
 * to the formatted [key] unless a custom name is provided at construction.
 *
 * @param namespace always `"nexo"` for Nexo items
 * @param key the raw Nexo item ID — e.g. `"ruby"`, `"amethyst_sword"`
 * @param customDisplayName optional override for [displayName]
 */
data class NexoItemKey(
    override val namespace: String,
    override val key: String,
    private val customDisplayName: String? = null
) : ItemKey {

    override val id: String get() = "$namespace:$key"

    override fun displayName(): String =
        customDisplayName
            ?: key.replace('_', ' ')
                .split(' ')
                .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }

    override fun toString(): String = id
}
