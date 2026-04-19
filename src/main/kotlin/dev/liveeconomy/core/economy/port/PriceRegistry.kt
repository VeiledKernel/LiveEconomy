package dev.liveeconomy.core.economy.port

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.data.model.MarketItem

/**
 * Internal port for item price registry access.
 *
 * Abstracts [dev.liveeconomy.core.economy.PriceServiceImpl] from its callers
 * so TradeServiceImpl and use cases depend on an interface, not a concrete class.
 *
 * // Internal interface — not part of public api/
 */
internal interface PriceRegistry {
    fun getItem(item: ItemKey): MarketItem?
    fun isListed(item: ItemKey): Boolean
}
