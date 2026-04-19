package dev.liveeconomy.util

import java.math.BigDecimal
import java.text.DecimalFormat

/**
 * Pure formatting utilities for monetary values.
 *
 * Stateless object — no interface needed (Rule 1 exception: stateless utility).
 * // No interface: stateless utility, never swapped
 */
object MoneyFormat {

    private val FULL  = DecimalFormat("#,##0.00")
    private val SHORT = DecimalFormat("#,##0.#")

    /** Format as full price: `1,234.56` */
    fun full(amount: Double): String      = FULL.format(amount)
    fun full(amount: BigDecimal): String  = FULL.format(amount)

    /**
     * Format as compact price with suffix:
     *   1_000     → `1K`
     *   1_500_000 → `1.5M`
     *   2_000_000_000 → `2B`
     */
    fun compact(amount: Double): String = when {
        amount >= 1_000_000_000 -> "${SHORT.format(amount / 1_000_000_000)}B"
        amount >= 1_000_000     -> "${SHORT.format(amount / 1_000_000)}M"
        amount >= 1_000         -> "${SHORT.format(amount / 1_000)}K"
        else                    -> FULL.format(amount)
    }

    /** Format with currency symbol prefix: `$1,234.56` */
    fun withSymbol(symbol: String, amount: Double): String = "$symbol${full(amount)}"

    /** Format a percentage: `+3.24%`, `-1.50%` */
    fun percent(value: Double): String {
        val sign = if (value >= 0) "+" else ""
        return "$sign${String.format("%.2f", value)}%"
    }
}
