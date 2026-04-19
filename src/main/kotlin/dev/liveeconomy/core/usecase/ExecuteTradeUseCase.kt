package dev.liveeconomy.core.usecase

import dev.liveeconomy.api.economy.result.TradeResult
import dev.liveeconomy.api.event.DomainEventBus
import dev.liveeconomy.api.event.TradeExecutedEvent
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.api.player.result.WithdrawResult
import dev.liveeconomy.api.storage.PortfolioStore
import dev.liveeconomy.api.storage.TransactionStore
import dev.liveeconomy.core.economy.port.PriceRegistry
import dev.liveeconomy.core.economy.port.TradePricingEngine
import dev.liveeconomy.core.usecase.port.InventoryGateway
import dev.liveeconomy.data.config.MarketConfig
import dev.liveeconomy.data.model.TradeAction
import dev.liveeconomy.data.model.Transaction
import java.util.UUID

/**
 * Orchestrates a complete market trade — the authoritative logic owner for
 * buy/sell flows.
 *
 * **No Bukkit imports.** All inventory and player interaction goes through
 * [InventoryGateway] (internal port). [WalletService] is also an interface.
 * This class is thread-agnostic by design — it performs no Bukkit API calls
 * directly, making it safely callable from any thread context and fully
 * testable without a running server.
 *
 * The platform-side adapter [dev.liveeconomy.platform.inventory.BukkitInventoryGateway]
 * handles the actual inventory mutations and MUST be called on the main thread.
 * The caller ([TradeServiceImpl]) is responsible for enforcing the thread contract.
 *
 * Coordinates (Rule 8 — 3+ services → Use Case):
 *  - [PriceRegistry]      — item lookup (internal port)
 *  - [TradePricingEngine] — price impact (internal port)
 *  - [InventoryGateway]   — inventory ops (internal port)
 *  - [WalletService]      — balance operations (api interface)
 *  - [PortfolioStore]     — holding mutations (api interface)
 *  - [TransactionStore]   — trade history (api interface)
 *  - [DomainEventBus]     — event dispatch (api interface)
 *
 * **Size budget:** 150 lines max. Any new cross-cutting concern (rate limiting,
 * audit logging, fee tiers) must be injected as a collaborator — not inlined.
 */
class ExecuteTradeUseCase(
    private val registry:  PriceRegistry,
    private val pricing:   TradePricingEngine,
    private val inventory: InventoryGateway,
    private val wallet:    WalletService,
    private val portfolio: PortfolioStore,
    private val txStore:   TransactionStore,
    private val eventBus:  DomainEventBus,
    private val config:    MarketConfig
) {

    // ── Buy ───────────────────────────────────────────────────────────────────

    fun executeBuy(playerUuid: UUID, item: ItemKey, quantity: Int): TradeResult {
        val marketItem = registry.getItem(item) ?: return TradeResult.NotListed
        if (quantity <= 0) return TradeResult.NotListed

        if (inventory.spaceFor(playerUuid, item) < quantity)
            return TradeResult.NoInventorySpace

        val cost = pricing.applyBuyImpact(marketItem, quantity, config.tradeTaxRate)
        val tax  = cost - (marketItem.askPrice * quantity)

        // WalletService is an interface — no Bukkit dependency here.
        // The player object the caller holds is only needed to pass to WalletService
        // which itself abstracts Vault/internal wallet behind its own interface.
        val balanceBefore = wallet.getBalance(playerUuid)
        if (balanceBefore < cost)
            return TradeResult.InsufficientFunds

        // Deduct balance — using UUID-based path avoids Bukkit Player reference
        if (wallet.withdraw(playerUuid, cost) is WithdrawResult.InsufficientFunds)
            return TradeResult.InsufficientFunds

        inventory.give(playerUuid, item, quantity)

        val held = portfolio.getHoldings(playerUuid).getOrDefault(item, 0)
        portfolio.setHolding(playerUuid, item, held + quantity)

        txStore.append(Transaction(playerUuid, System.currentTimeMillis(), item,
            TradeAction.BUY, quantity, cost / quantity, cost))

        eventBus.publish(TradeExecutedEvent(playerUuid, item, TradeAction.BUY,
            quantity, marketItem.currentPrice, cost, tax, marketItem.currentPrice))

        return TradeResult.Success(total = cost, newPrice = marketItem.currentPrice, taxPaid = tax)
    }

    // ── Sell ──────────────────────────────────────────────────────────────────

    fun executeSell(playerUuid: UUID, item: ItemKey, quantity: Int): TradeResult {
        val marketItem = registry.getItem(item) ?: return TradeResult.NotListed
        if (quantity <= 0) return TradeResult.NotListed

        if (inventory.countHeld(playerUuid, item) < quantity)
            return TradeResult.InsufficientItems

        val revenue = pricing.applySellImpact(marketItem, quantity, config.tradeTaxRate)
        val tax     = (marketItem.bidPrice * quantity) - revenue

        inventory.take(playerUuid, item, quantity)
        wallet.deposit(playerUuid, revenue)

        val held    = portfolio.getHoldings(playerUuid).getOrDefault(item, 0)
        val newHeld = (held - quantity).coerceAtLeast(0)
        if (newHeld == 0) portfolio.removeHolding(playerUuid, item)
        else              portfolio.setHolding(playerUuid, item, newHeld)

        portfolio.addPnl(playerUuid,
            java.math.BigDecimal.valueOf(revenue - marketItem.askPrice * quantity))

        txStore.append(Transaction(playerUuid, System.currentTimeMillis(), item,
            TradeAction.SELL, quantity, revenue / quantity, revenue))

        eventBus.publish(TradeExecutedEvent(playerUuid, item, TradeAction.SELL,
            quantity, marketItem.currentPrice, revenue, tax, marketItem.currentPrice))

        return TradeResult.Success(total = revenue, newPrice = marketItem.currentPrice, taxPaid = tax)
    }
}
