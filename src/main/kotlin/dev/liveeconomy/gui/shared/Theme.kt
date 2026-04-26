package dev.liveeconomy.gui.shared

import org.bukkit.Material

/**
 * Central design token file for LiveEconomy GUIs.
 *
 * Every color, symbol, and material used in any GUI should reference
 * a constant here — never hardcode §a or Material.BLACK_STAINED_GLASS_PANE
 * directly in a GUI file. This makes a full visual restyle a one-file change.
 */
object Theme {

    // ── Color codes ───────────────────────────────────────────────────────────

    /** Gold — all currency values, prices */
    const val GOLD = "§6"

    /** White — item names, section titles */
    const val WHITE = "§f"

    /** Bold modifier */
    const val BOLD = "§l"

    /** Green — price going up, profit, buy action, confirm */
    const val GREEN = "§a"

    /** Red — price going down, loss, sell action, cancel */
    const val RED = "§c"

    /** Yellow — alerts, warnings, highlight */
    const val YELLOW = "§e"

    /** Aqua — headers, special labels */
    const val AQUA = "§b"

    /** Light purple — portfolio, personal stats */
    const val PURPLE = "§d"

    /** Gray — muted text, hints, separators */
    const val GRAY = "§7"

    /** Dark gray — very muted, action hints */
    const val DARK_GRAY = "§8"

    /** Reset — clears formatting */
    const val RESET = "§r"

    // ── Separator lines ───────────────────────────────────────────────────────

    /** Short separator — item lore divider */
    const val SEP = "§8§m                    §r"

    /** Invisible separator — blank lore line */
    const val BLANK = "§r"

    // ── Trend symbols ─────────────────────────────────────────────────────────

    const val TREND_UP   = "▲"
    const val TREND_DOWN = "▼"
    const val TREND_FLAT = "→"

    // ── Border materials ──────────────────────────────────────────────────────

    val BORDER_MAT   = Material.BLACK_STAINED_GLASS_PANE
    val FILLER_MAT   = Material.GRAY_STAINED_GLASS_PANE
    val CONFIRM_MAT  = Material.LIME_STAINED_GLASS_PANE
    val CANCEL_MAT   = Material.RED_STAINED_GLASS_PANE
    val DISABLED_MAT = Material.GRAY_STAINED_GLASS_PANE
    val TAB_ACTIVE   = Material.LIME_STAINED_GLASS_PANE
    val TAB_INACTIVE = Material.GRAY_STAINED_GLASS_PANE

    // ── Formatting helpers ────────────────────────────────────────────────────

    /** Format a price value with currency symbol and gold color. */
    fun price(symbol: String, amount: Double): String =
        "$GOLD$symbol${dev.liveeconomy.utils.ChatUtil.formatPrice(amount)}"

    /**
     * Format a price change percentage with colored trend arrow.
     * Returns e.g. "§a▲ +9.40%" or "§c▼ −3.10%" or "§7→ 0.00%"
     */
    fun change(pct: Double): String {
        return when {
            pct > 0.005  -> "$GREEN$TREND_UP +${String.format("%.2f", pct)}%"
            pct < -0.005 -> "$RED$TREND_DOWN ${String.format("%.2f", pct)}%"
            else         -> "${GRAY}$TREND_FLAT ${String.format("%.2f", Math.abs(pct))}%"
        }
    }

    /**
     * Color a value based on whether it is positive, negative, or zero.
     */
    fun signed(value: Double, formatted: String): String = when {
        value > 0.0  -> "$GREEN+$formatted"
        value < 0.0  -> "$RED$formatted"
        else         -> "$GRAY$formatted"
    }

    /** Build a clean section header label. */
    fun header(text: String): String = "$DARK_GRAY$BOLD$text"

    /** Build a key-value lore line: "§7Key  §f Value" */
    fun kv(key: String, value: String): String = "$GRAY$key  $WHITE$value"

    /** Hint line — dark gray, used for click instructions */
    fun hint(text: String): String = "$DARK_GRAY$text"
}
