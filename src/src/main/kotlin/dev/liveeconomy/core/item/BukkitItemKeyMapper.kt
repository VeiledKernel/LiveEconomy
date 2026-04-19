package dev.liveeconomy.core.item

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.item.ItemKeyMapper
import org.bukkit.Material

/**
 * The ONLY implementation of [ItemKeyMapper] in the codebase.
 *
 * All Material ↔ ItemKey conversions go through this class. No other class
 * may call [Material.matchMaterial], [Material.getKey], or equivalent when
 * an [ItemKey] is the target type.
 *
 * Construction: injected into any class that crosses the Material/ItemKey
 * boundary. Never instantiated outside the composition root.
 *
 * @param nexoAvailable whether the Nexo plugin is loaded. If false,
 *   [fromNexoId] throws [IllegalStateException].
 */
class BukkitItemKeyMapper(
    private val nexoAvailable: Boolean
) : ItemKeyMapper {

    override fun fromMaterial(material: Material): ItemKey =
        VanillaItemKey(
            namespace = "minecraft",
            key       = material.key.key   // e.g. "diamond", "iron_ingot"
        )

    override fun toMaterial(key: ItemKey): Material? {
        if (key.namespace != "minecraft") return null
        return Material.matchMaterial(key.key.uppercase())
    }

    override fun fromNexoId(nexoId: String): ItemKey {
        check(nexoAvailable) {
            "Cannot create NexoItemKey for '$nexoId' — Nexo is not installed."
        }
        return NexoItemKey(namespace = "nexo", key = nexoId)
    }

    /**
     * Convenience: parse an [ItemKey] from a raw string in `namespace:key` format.
     * Falls back to `minecraft` namespace if no colon is present.
     */
    fun fromString(raw: String): ItemKey {
        val parts = raw.split(':', limit = 2)
        return if (parts.size == 2) {
            when (parts[0]) {
                "minecraft" -> {
                    val mat = Material.matchMaterial(parts[1].uppercase())
                        ?: error("Unknown vanilla material: '${parts[1]}'")
                    fromMaterial(mat)
                }
                "nexo" -> fromNexoId(parts[1])
                else   -> error("Unknown namespace '${parts[0]}' in item key '$raw'")
            }
        } else {
            // No namespace prefix — assume minecraft
            val mat = Material.matchMaterial(raw.uppercase())
                ?: error("Unknown material '$raw' and no namespace prefix given")
            fromMaterial(mat)
        }
    }
}
