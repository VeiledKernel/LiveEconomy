package dev.liveeconomy.api.economy

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI
import dev.liveeconomy.api.item.ItemKey

/**
 * Read-only price queries for the LiveEconomy market.
 *
 * All methods are safe to call from any thread — implementations are
 * required to be thread-agnostic (Rule 7).
 *
 * Inject this interface when you only need price data:
 * ```kotlin
 * class PriceBoardPlaceholder(private val price: PriceService)
 * ```
 *
 * Access via:
 * ```kotlin
 * val price = LiveEconomyAPI.get().price()
 * ```
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
interface PriceService {

    /**
     * Current mid-market price for [item].
     * Returns null if [item] is not listed on this market.
     */
    fun getPrice(item: ItemKey): Double?

    /**
     * Current bid price (highest price a buyer will pay) for [item].
     * Slightly below mid-market. Returns null if item is not listed.
     */
    fun getBid(item: ItemKey): Double?

    /**
     * Current ask price (lowest price a seller will accept) for [item].
     * Slightly above mid-market. Returns null if item is not listed.
     */
    fun getAsk(item: ItemKey): Double?

    /**
     * Composite market index across all listed items.
     * 1000.0 = all items at base price. >1000 = bull, <1000 = bear.
     */
    fun getIndex(): Double

    /**
     * Percentage price change since the last market tick.
     * Returns null if [item] is not listed.
     */
    fun getPriceChangePercent(item: ItemKey): Double?

    /**
     * Whether [item] is currently listed and tradable on this market.
     */
    fun isListed(item: ItemKey): Boolean

    /**
     * All currently listed item keys.
     */
    fun getListedItems(): Set<ItemKey>
}
