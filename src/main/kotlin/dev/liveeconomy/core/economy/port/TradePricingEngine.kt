package dev.liveeconomy.core.economy.port

import dev.liveeconomy.data.model.MarketItem

/**
 * Internal port for price impact calculation.
 *
 * Abstracts [dev.liveeconomy.core.economy.PriceModelImpl] from its callers.
 *
 * // Internal interface — not part of public api/
 */
internal interface TradePricingEngine {
    fun applyBuyImpact(item: MarketItem, quantity: Int, taxRate: Double): Double
    fun applySellImpact(item: MarketItem, quantity: Int, taxRate: Double): Double
}
