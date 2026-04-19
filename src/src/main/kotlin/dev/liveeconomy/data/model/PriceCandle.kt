package dev.liveeconomy.data.model

/**
 * One OHLCV candlestick capturing price movement over one market tick.
 *
 * Immutable value object — no Bukkit or plugin dependencies.
 */
data class PriceCandle(
    val open:      Double,
    val high:      Double,
    val low:       Double,
    val close:     Double,
    val volume:    Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isBullish: Boolean get() = close >= open
    val bodySize:  Double  get() = Math.abs(close - open)
    val range:     Double  get() = high - low
}
