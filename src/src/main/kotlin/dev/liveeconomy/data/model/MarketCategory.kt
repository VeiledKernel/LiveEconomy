package dev.liveeconomy.data.model

/**
 * Category identity for a market item.
 *
 * In v4, categories are config-driven — any string ID is valid.
 * This data class wraps the raw ID with a display name.
 * Category IDs match the filenames in categories/ (without .yml).
 */
data class MarketCategory(
    /** Normalised lowercase ID matching the category filename: "gems", "mob" */
    val id:          String,

    /** Display name from categories/<id>.yml → category.display-name */
    val displayName: String
) {
    companion object {
        /**
         * Normalise a raw category string from config or storage.
         * e.g. "Mob Drops" → "mob_drops", "MOB-DROPS" → "mob_drops"
         */
        fun normalise(raw: String): String =
            raw.lowercase().replace('-', '_').replace(' ', '_').trim()
    }
}
