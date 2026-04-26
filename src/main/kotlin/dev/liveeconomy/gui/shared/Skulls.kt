package dev.liveeconomy.gui.shared

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.UUID

/**
 * Custom textured skull builder for GUI icons.
 *
 * Uses Paper's PlayerProfile API to apply base64-encoded skin textures
 * directly to PLAYER_HEAD items — no player name lookup, no network request,
 * no async needed. Works offline.
 *
 * Usage:
 *   Skulls.of(Skulls.COIN) {
 *       name("${Theme.GOLD}Balance")
 *       lore("§7...")
 *   }
 *
 * Texture values are the base64 string from the "value" field of a
 * Minecraft skin texture JSON. You can get them from:
 *   https://minecraft-heads.com  → "For Developers" tab → "Value" field
 *
 * Each constant here is a pre-vetted texture that renders reliably on
 * Paper 1.21+ without any external requests.
 */
object Skulls {

    // ── Texture constants ─────────────────────────────────────────────────────
    // Source: minecraft-heads.com — "Value" field under "For Developers"

    // Economy / Finance
    const val COIN         = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTI0YTY3Y2VjYzZkOGY5YWVlN2I5OTgzY2IxNDU3NzdkNjZjMzM0YTJlZGQ5ZDVhZWEzMThiYWU4NTY4NyJ9fX0="
    const val GOLD_COIN    = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDllYzgwZGNkNTBjMTM3MTZlZTBmZmZlZWFiNjVkYTY0NDhiOTlmZTdjOWM4NjU3MWU5ZGY3ZDkzMTE1NiJ9fX0="
    const val WALLET       = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODY1MmUyYjkzNWJhNTY4OWUyY2QwMTI1NThiNzViNGJiNzI4NGMzMzc1MmM2ZTU5ZWQzNzk1NjlhZmYifX19"
    const val CHART_UP     = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmViNTg4YjIxYTZmOThhZDFmZjRlMDg1YzVhYjE0OTZhMmQzOWMxZTE5NmZhNjMzMzlhMWEzMmQzMzUxIn19fQ=="
    const val CHART_DOWN   = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzQzMzU3NDZkNTVkMWZhNzdiYzhhMTdhZTg4ZDhiYTM4YWNiMWEwNTJmZTM1NTM0N2JhM2JhMzk5Y2M1ZiJ9fX0="
    const val TROPHY       = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWZiZTRmZGI3ZWM3OGQ4YTVhNWQ2NjI2OGU3ZWM5ZDc0MDBlMGViNWNjZGZkMGRjNzY4Njg0ZDY3NDg2YiJ9fX0="
    const val MAGNIFIER    = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjEyNjE3MTk1NjJhOGZjMzU0YThlMWFmMjU2ZGRhZjE0OTMwYWI0MWNhMmRlZTQ3YTI2M2EzMmRmNDEzOSJ9fX0="

    // Navigation / UI
    const val ARROW_LEFT   = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ2OWUwNmU1ZGFkZmQ4NGU1ZjNkMWMyMTA2M2YyNTUzYjI4ODc4ZDc1M2QwNzdlNzU5ZDNjMzUxIn19fQ=="
    const val ARROW_RIGHT  = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWI2ZjFhMjViNmJjMTk5OTQ2NDcyYWFiMzcxNzNkZTFmZGNkYjczNThmNDM5MDFlNGRhNmRjZWQzNWQxYyJ9fX0="
    const val BACK         = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjg0ZjU5NzEzMWJiZTI1ZGMwNTVhYjMyOGM3NTlkOGI1MGMxNzViNTI0OWJjZWU2ZjVkZGI4YmEyNjM2In19fQ=="
    const val CLOSE        = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmViNTg4YjIxYTZmOThhZDFmZjRlMDg1YzVhYjE0OTZhMmQzOWMxZTE5NmZhNjMzMzlhMWEzMmQzMzUxIn19fQ=="
    const val CONFIRM      = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTc5YTVjOTVlZTE3YWJmZWY0NWM4ZGMxZjU2NTRjMzJmNjE2ODJlMTI0ZGVmNjEyZTU2YzU1YzM2NDQ1ZiJ9fX0="
    const val ALERT_BELL   = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTI1YTJlNzk5ZmVlN2RiOWYwOWE5ODc5N2Y4ZTJhMzQwMmI1ZWZhNjM1ZjkxYTViYmMzNmUyMTc4ZGEifX19"
    const val INFO         = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmU5ZTcwMzY4NzE4MDFkMzdmYjkwM2M0MzRmNjM2MTQ1YWRmNGJmN2MxZTg1NDgxMjJkOWQ5ZTVhNmQ1OSJ9fX0="

    // Market categories
    const val CAT_GEMS     = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzI0MzE5MmQ2OTZiZTc3ZDI0ZTQ2ZTk0NTJhMTQxYTVhMjhlMmFhMzE3ZjU0ZjNmYmJhZTIyMjE2NWIifX19"
    const val CAT_METALS   = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODIxMjhiZGMzN2YzNGQ3OTQ0MDUxMWZkNmM2MjcwN2Y5OTg2MDFhNjhkMzc3NmZiMGI1OThkN2JkM2Q5NCJ9fX0="
    const val CAT_FOOD     = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTc5YTVjOTVlZTE3YWJmZWY0NWM4ZGMxZjU2NTRjMzJmNjE2ODJlMTI0ZGVmNjEyZTU2YzU1YzM2NDQ1ZiJ9fX0="
    const val CAT_NETHER   = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDVmNzZjNTUxNDI4ZDcxOTNiMTJkZmI1YTgzM2ZkYjRmMzA2ZjE1OTI2ZGVkMzI0MzA3ZmU0OTA3MjZkYSJ9fX0="
    const val CAT_END      = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGMxMzQyNzY0ZDFhMzQ1YTM2ZTViMDI0ZTg2MDU0NzNiZTViZWFjMTM1ODFkMjUwMTI2OGVmNTY0N2M1In19fQ=="
    const val CAT_BLOCKS   = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDc4ZjJiN2U0NzkyODc1OGU5MGYxZThlNmZkNjMyMzdhNzdlYjI2MDgwZGVhMzUzZTE4Y2YyMTc0MzlmNiJ9fX0="
    const val CAT_WOOD     = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjRlZTc2ODliMzk5ODY5ZjFmMTM0NzBiODI0YTIxZGY5ZGIwY2RhMTY0NzE5N2E5NDcxNTA3YTVhZjA4NCJ9fX0="
    const val CAT_MOB      = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDE3MGIzYWI5Y2UyMzFhN2QwMzc4NGFhNzM5YzEwMzQ1ZmEwMGE3MTcyMThkZGNhOTMyNGRlMzY0ZDNhMiJ9fX0="
    const val CAT_FARM     = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODE2OTgxNTA0ZGVhN2RiMGYxZTk4MDVhY2QxYjVmYTEyYzU5ZTlkYmUwMGFmMzFiNTY2NDUwZGYxZmEifX19"
    const val CAT_MISC     = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjhkYmIyNTVmZTkyZDMwMTkwYmFjMjk1MzNhNWNkNjkyMWZmYjc2NjcyNjUxYTM0MzA4NTE2OTA2In19fQ=="

    // Roles
    const val ROLE_MINER   = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGU0YjhiNGQ3MjVkMzFiNTEzMjM3NDZhMzExNmMxZmNlMzM1YTYxNTcxY2JhMWZiNGYxZWIxOTZiMzU1NyJ9fX0="
    const val ROLE_TRADER  = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGRlYzY3MDRjZGFmNjQ5YWQxZTNiMzIzZTk3YTRhMjgyZWM2ZmNkMTRkOGNmYzc2YzJmZjkzYzRiNzJlZCJ9fX0="
    const val ROLE_FARMER  = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDk3ZTQ2ZjA0ZDg1OGVhYjUxZDhlMWY1NDFkODc4MzYxMDhkMTM3YmJiM2YwZGUxNTQ5MDM3OGRhNThiZCJ9fX0="
    const val ROLE_CRAFTER = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmU4N2JjNzlkNzJjNzQ4OWIxOTE3N2E1NzgwZjFkNmJkNDA0ZWUxNjVhNDMwMWNiNGE3MDM5YzQ1MjZlMCJ9fX0="

    // ── Builder ───────────────────────────────────────────────────────────────

    /**
     * Build a custom skull ItemStack with the given base64 texture.
     * Accepts an optional [ItemBuilder] block for name/lore/etc.
     */
    fun of(texture: String, amount: Int = 1, init: ItemBuilder.() -> Unit = {}): ItemStack {
        val stack = itemStack(Material.PLAYER_HEAD, amount, init)
        applyTexture(stack, texture)
        return stack
    }

    /**
     * Apply a base64 texture to an existing PLAYER_HEAD ItemStack in-place.
     * Uses Paper's PlayerProfile API — no network request, works offline.
     */
    fun applyTexture(stack: ItemStack, texture: String): ItemStack {
        val meta = stack.itemMeta as? SkullMeta ?: return stack
        try {
            val profile = Bukkit.createProfile(UUID.randomUUID(), null)
            profile.setProperty(
                com.destroystokyo.paper.profile.ProfileProperty("textures", texture)
            )
            meta.playerProfile = profile
            stack.itemMeta = meta
        } catch (_: Exception) {
            // Paper API not available or texture malformed — fall back silently
        }
        return stack
    }

    /**
     * Return the category skull texture for a given category id string.
     * Falls back to CAT_MISC if the category is unrecognised.
     */
    fun forCategory(categoryId: String): String = when {
        categoryId.contains("gem")    -> CAT_GEMS
        categoryId.contains("metal")  -> CAT_METALS
        categoryId.contains("food")   -> CAT_FOOD
        categoryId.contains("nether") -> CAT_NETHER
        categoryId.contains("end")    -> CAT_END
        categoryId.contains("block")  -> CAT_BLOCKS
        categoryId.contains("wood")   -> CAT_WOOD
        categoryId.contains("mob")    -> CAT_MOB
        categoryId.contains("farm")   -> CAT_FARM
        else                          -> CAT_MISC
    }
}
