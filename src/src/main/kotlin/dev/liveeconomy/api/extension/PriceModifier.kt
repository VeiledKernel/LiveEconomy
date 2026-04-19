package dev.liveeconomy.api.extension

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI
import dev.liveeconomy.api.item.ItemKey

/**
 * Extension point for custom price modifiers.
 *
 * Implementations are registered at startup via the composition root:
 * ```kotlin
 * PriceModelImpl(modifiers = listOf(VipPriceModifier(roleService), TaxModifier(config)))
 * ```
 *
 * Called during every price calculation — must be fast, no I/O.
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
fun interface PriceModifier {
    /**
     * Adjust [basePrice] for [item] and return the modified price.
     * Return [basePrice] unchanged to pass through without modification.
     */
    fun modify(item: ItemKey, basePrice: Double): Double
}
