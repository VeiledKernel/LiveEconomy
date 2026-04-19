package dev.liveeconomy.data.config

/**
 * Typed representation of gui.yml market layout configuration.
 *
 * Constructed once by [GuiLayoutLoader] at startup/reload. Injected into
 * [GuiFactory] and all GUI classes — no raw YAML reads at render time.
 *
 * Materials are stored as strings here (pure data — no Bukkit dependency).
 * GUI classes resolve them to [org.bukkit.Material] at render time.
 */
data class GuiConfig(
    val title:              String,
    val rows:               Int,
    val borderMaterialName: String,   // e.g. "BLACK_STAINED_GLASS_PANE"
    val fillerMaterialName: String,   // e.g. "GRAY_STAINED_GLASS_PANE"
    val itemSlots:      IntArray,
    val borderSlots:    IntArray,
    val buttons:        ButtonSlots
) {
    data class ButtonSlots(
        val prevPage:    Int,
        val nextPage:    Int,
        val search:      Int,
        val title:       Int,
        val alerts:      Int,
        val wallet:      Int,
        val portfolio:   Int,
        val orders:      Int,
        val index:       Int,
        val leaderboard: Int
    )

    /**
     * Resolve the title for display — replaces {category} placeholder and
     * converts & colour codes to §.
     */
    fun resolveTitle(categoryDisplayName: String): String =
        title
            .replace("{category}", categoryDisplayName)
            .replace("&", "§")
}
