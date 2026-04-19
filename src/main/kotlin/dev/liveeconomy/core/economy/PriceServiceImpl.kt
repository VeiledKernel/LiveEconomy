package dev.liveeconomy.core.economy

import dev.liveeconomy.api.economy.PriceService
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.core.economy.port.PriceRegistry
import dev.liveeconomy.core.market.MarketRegistry

/**
 * [PriceService] implementation — API-facing read surface only.
 *
 * Single responsibility: answer public price queries.
 * Registry ownership → [MarketRegistry]
 * Tick lifecycle     → [dev.liveeconomy.core.market.MarketTicker]
 * Persistence        → [MarketRegistry.persistAllPrices]
 *
 * Also implements [PriceRegistry] (internal port) so it can be injected
 * into [TradeServiceImpl] and use cases without them depending on a concrete class.
 */
class PriceServiceImpl(
    private val registry: MarketRegistry
) : PriceService, PriceRegistry {

    // ── PriceService (public API) ─────────────────────────────────────────────

    override fun getPrice(item: ItemKey): Double?              = registry.getItem(item)?.currentPrice
    override fun getBid(item: ItemKey): Double?                = registry.getItem(item)?.bidPrice
    override fun getAsk(item: ItemKey): Double?                = registry.getItem(item)?.askPrice
    override fun isListed(item: ItemKey): Boolean              = registry.isListed(item)
    override fun getListedItems(): Set<ItemKey>                 = registry.getAllItems().values.map { it.itemKey }.toSet()
    override fun getPriceChangePercent(item: ItemKey): Double? = registry.getItem(item)?.priceChangePercent
    override fun getIndex(): Double                            = registry.allItems()
        .let { items -> if (items.isEmpty()) 1000.0 else items.map { it.currentPrice / it.basePrice }.average() * 1000.0 }

    // ── PriceRegistry (internal port) ─────────────────────────────────────────

    override fun getItem(item: ItemKey) = registry.getItem(item)
}
