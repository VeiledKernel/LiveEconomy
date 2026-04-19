package dev.liveeconomy.api.item

/**
 * Stable, version-safe identity for any tradable item.
 *
 * Replaces [org.bukkit.Material] in the `api/` layer. External plugins
 * and storage use [ItemKey] instead of Bukkit's Material enum, which can
 * change across Minecraft versions or diverge for custom-item plugins.
 *
 * Format: `namespace:key`
 *   - Vanilla: `minecraft:diamond`
 *   - Nexo:    `nexo:ruby`
 *
 * **Equality contract (Rule 12):** Two [ItemKey] instances are equal if
 * and only if their [id] values are equal. Implementations must be
 * `data class` to guarantee correct `equals`/`hashCode` in maps and caches.
 *
 * @since 4.0 (Experimental — see ExperimentalLiveEconomyAPI)
 */
interface ItemKey {

    /** Full identifier: `"$namespace:$key"` — e.g. `"minecraft:diamond"` */
    val id: String

    /** Owning system: `"minecraft"`, `"nexo"`, etc. */
    val namespace: String

    /** Item name within the namespace: `"diamond"`, `"ruby"`, etc. */
    val key: String

    /**
     * Human-readable display name derived from [key].
     * Override in implementations for custom formatting.
     */
    fun displayName(): String =
        key.replace('_', ' ')
            .split(' ')
            .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }
}
