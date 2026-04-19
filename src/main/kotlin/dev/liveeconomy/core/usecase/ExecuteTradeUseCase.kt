package dev.liveeconomy.core.usecase

import dev.liveeconomy.api.economy.result.TradeResult
import dev.liveeconomy.api.event.DomainEventBus
import dev.liveeconomy.api.event.TradeExecutedEvent
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.api.player.result.WithdrawResult
import dev.liveeconomy.api.storage.PortfolioStore
import dev.liveeconomy.api.storage.TransactionStore
import dev.liveeconomy.core.economy.port.PriceRegistry
import dev.liveeconomy.core.economy.port.TradePricingEngine
import dev.liveeconomy.data.config.MarketConfig
import dev.liveeconomy.data.model.TradeAction
import dev.liveeconomy.data.model.Transaction
import dev.liveeconomy.util.InventoryUtil
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Orchestrates a complete market trade — the real logic owner for buy/sell flows.
 *
 * Coordinates 6 collaborators (Rule 8 — 3+ services → Use Case):
 *  - [PriceRegistry]      — item lookup, price reads
 *  - [TradePricingEngine] — price impact calculation
 *  - [WalletService]      — balance operations (interface)
 *  - [PortfolioStore]     — holding mutations (interface)
 *  - [TransactionStore]   — trade history recording (interface)
 *  - [DomainEventBus]     — post-trade event dispatch (interface)
 *
 * [TradeServiceImpl] delegates to this — it does not contain this logic.
 * Must be called on the main thread (inventory + wallet are Bukkit-bound).
 */
class ExecuteTradeUseCase(
    private val registry:  PriceRegistry,
    private val pricing:   TradePricingEngine,
    private val wallet:    WalletService,
    private val portfolio: PortfolioStore,
    private val txStore:   TransactionStore,
    private val eventBus:  DomainEventBus,
    private val mapper:    ItemKeyMapper,
    private val config:    MarketConfig
) {

    fun executeBuy(player: Player, item: ItemKey, quantity: Int): TradeResult {
        val marketItem = registry.getItem(item) ?: return TradeResult.NotListed
        if (quantity <= 0) return TradeResult.NotListed

        val material = mapper.toMaterial(item)
        if (material != null && InventoryUtil.spaceFor(player, material) < quantity)
            return TradeResult.NoInventorySpace

        val cost = pricing.applyBuyImpact(marketItem, quantity, config.tradeTaxRate)
        val tax  = cost - (marketItem.askPrice * quantity)

        if (wallet.withdraw(player, cost) is WithdrawResult.InsufficientFunds)
            return TradeResult.InsufficientFunds

        material?.let { player.inventory.addItem(ItemStack(it, quantity)) }

        val held = portfolio.getHoldings(player.uniqueId).getOrDefault(item, 0)
        portfolio.setHolding(player.uniqueId, item, held + quantity)

        txStore.append(Transaction(player.uniqueId, System.currentTimeMillis(), item,
            TradeAction.BUY, quantity, cost / quantity, cost))

        eventBus.publish(TradeExecutedEvent(player.uniqueId, item, TradeAction.BUY,
            quantity, marketItem.currentPrice, cost, tax, marketItem.currentPrice))

        return TradeResult.Success(total = cost, newPrice = marketItem.currentPrice, taxPaid = tax)
    }

    fun executeSell(player: Player, item: ItemKey, quantity: Int): TradeResult {
        val marketItem = registry.getItem(item) ?: return TradeResult.NotListed
        if (quantity <= 0) return TradeResult.NotListed

        val material = mapper.toMaterial(item)
        if (material != null && InventoryUtil.countInInventory(player, material) < quantity)
            return TradeResult.InsufficientItems

        val revenue = pricing.applySellImpact(marketItem, quantity, config.tradeTaxRate)
        val tax     = (marketItem.bidPrice * quantity) - revenue

        material?.let {
            var rem = quantity
            for (stack in player.inventory.contents.filterNotNull()) {
                if (stack.type == it && rem > 0) { val r = minOf(stack.amount, rem); stack.amount -= r; rem -= r }
            }
        }

        wallet.deposit(player, revenue)

        val held = portfolio.getHoldings(player.uniqueId).getOrDefault(item, 0)
        val newHeld = (held - quantity).coerceAtLeast(0)
        if (newHeld == 0) portfolio.removeHolding(player.uniqueId, item)
        else              portfolio.setHolding(player.uniqueId, item, newHeld)

        portfolio.addPnl(player.uniqueId,
            java.math.BigDecimal.valueOf(revenue - marketItem.askPrice * quantity))

        txStore.append(Transaction(player.uniqueId, System.currentTimeMillis(), item,
            TradeAction.SELL, quantity, revenue / quantity, revenue))

        eventBus.publish(TradeExecutedEvent(player.uniqueId, item, TradeAction.SELL,
            quantity, marketItem.currentPrice, revenue, tax, marketItem.currentPrice))

        return TradeResult.Success(total = revenue, newPrice = marketItem.currentPrice, taxPaid = tax)
    }
}
