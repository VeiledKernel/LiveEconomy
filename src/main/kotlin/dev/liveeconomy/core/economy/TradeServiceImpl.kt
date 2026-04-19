package dev.liveeconomy.core.economy

import dev.liveeconomy.api.economy.TradeService
import dev.liveeconomy.api.economy.result.OrderResult
import dev.liveeconomy.api.economy.result.ShortResult
import dev.liveeconomy.api.economy.result.TradeResult
import dev.liveeconomy.api.event.DomainEventBus
import dev.liveeconomy.api.event.TradeExecutedEvent
import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.api.player.result.WithdrawResult
import dev.liveeconomy.api.storage.PortfolioStore
import dev.liveeconomy.data.config.MarketConfig
import dev.liveeconomy.data.model.ShortPosition
import dev.liveeconomy.data.model.TradeAction
import dev.liveeconomy.util.InventoryUtil
import org.bukkit.entity.Player

/**
 * [TradeService] implementation — all market write operations.
 *
 * **Threading contract (permanent for v4.x):** All methods must be called
 * on the main server thread. Enforced at the call site — do not remove this
 * constraint without a full async API redesign.
 *
 * Depends on:
 * - [PriceServiceImpl] for item registry and price state
 * - [PriceModelImpl] for price impact calculation
 * - [OrderBook] for limit order management
 * - [WalletService] for balance operations
 * - [PortfolioStore] for holdings and short positions
 * - [DomainEventBus] for post-trade event dispatch
 */
class TradeServiceImpl(
    private val prices:    PriceServiceImpl,
    private val model:     PriceModelImpl,
    private val orders:    OrderBook,
    private val wallet:    WalletService,
    private val portfolio: PortfolioStore,
    private val eventBus:  DomainEventBus,
    private val config:    MarketConfig,
    private val mapper:    ItemKeyMapper
) : TradeService {

    // ── Buy ───────────────────────────────────────────────────────────────────

    override fun executeBuy(player: Player, item: ItemKey, quantity: Int): TradeResult {
        val marketItem = prices.getItem(item) ?: return TradeResult.NotListed
        if (quantity <= 0) return TradeResult.NotListed

        // Check inventory space before withdrawing money
        val material = mapper.toMaterial(item)
        if (material != null && InventoryUtil.spaceFor(player, material) < quantity)
            return TradeResult.NoInventorySpace

        val taxRate = config.tradeTaxPercent / 100.0
        val cost    = model.applyBuyImpact(marketItem, quantity, taxRate)
        val tax     = cost - (marketItem.askPrice * quantity)

        val withdraw = wallet.withdraw(player, cost)
        if (withdraw is WithdrawResult.InsufficientFunds)
            return TradeResult.InsufficientFunds

        // Deliver items
        if (material != null) {
            player.inventory.addItem(
                org.bukkit.inventory.ItemStack(material, quantity)
            )
        }

        portfolio.setHolding(
            player.uniqueId, item,
            portfolio.getHoldings(player.uniqueId).getOrDefault(item, 0) + quantity
        )

        val event = TradeExecutedEvent(
            playerUuid = player.uniqueId,
            item       = item,
            action     = TradeAction.BUY,
            quantity   = quantity,
            unitPrice  = marketItem.currentPrice,
            total      = cost,
            taxPaid    = tax,
            newPrice   = marketItem.currentPrice
        )
        eventBus.publish(event)

        return TradeResult.Success(
            total    = cost,
            newPrice = marketItem.currentPrice,
            taxPaid  = tax
        )
    }

    // ── Sell ──────────────────────────────────────────────────────────────────

    override fun executeSell(player: Player, item: ItemKey, quantity: Int): TradeResult {
        val marketItem = prices.getItem(item) ?: return TradeResult.NotListed
        if (quantity <= 0) return TradeResult.NotListed

        val material = mapper.toMaterial(item)
        if (material != null && InventoryUtil.countInInventory(player, material) < quantity)
            return TradeResult.InsufficientItems

        val taxRate  = config.tradeTaxPercent / 100.0
        val revenue  = model.applySellImpact(marketItem, quantity, taxRate)
        val tax      = (marketItem.bidPrice * quantity) - revenue

        // Remove items from inventory
        if (material != null) {
            var remaining = quantity
            for (stack in player.inventory.contents.filterNotNull()) {
                if (stack.type == material) {
                    val remove = minOf(stack.amount, remaining)
                    stack.amount -= remove
                    remaining   -= remove
                    if (remaining == 0) break
                }
            }
        }

        wallet.deposit(player, revenue)

        val held = portfolio.getHoldings(player.uniqueId).getOrDefault(item, 0)
        val newHeld = (held - quantity).coerceAtLeast(0)
        if (newHeld == 0) portfolio.removeHolding(player.uniqueId, item)
        else              portfolio.setHolding(player.uniqueId, item, newHeld)

        val event = TradeExecutedEvent(
            playerUuid = player.uniqueId,
            item       = item,
            action     = TradeAction.SELL,
            quantity   = quantity,
            unitPrice  = marketItem.currentPrice,
            total      = revenue,
            taxPaid    = tax,
            newPrice   = marketItem.currentPrice
        )
        eventBus.publish(event)

        return TradeResult.Success(
            total    = revenue,
            newPrice = marketItem.currentPrice,
            taxPaid  = tax
        )
    }

    // ── Limit orders ──────────────────────────────────────────────────────────

    override fun placeLimitOrder(
        player:      Player,
        item:        ItemKey,
        quantity:    Int,
        targetPrice: Double,
        isBuyOrder:  Boolean
    ): OrderResult {
        if (!prices.isListed(item)) return OrderResult.NotListed
        return orders.place(player, item, quantity, targetPrice, isBuyOrder)
    }

    override fun cancelLimitOrder(player: Player, orderId: String): OrderResult =
        orders.cancel(player, orderId)

    // ── Short selling ─────────────────────────────────────────────────────────

    override fun openShort(player: Player, item: ItemKey, quantity: Int): ShortResult {
        if (!config.allowShortSelling) return ShortResult.Disabled
        val marketItem = prices.getItem(item) ?: return ShortResult.NotListed

        val existing = portfolio.getShortPositions(player.uniqueId)[item]
        if (existing != null) return ShortResult.AlreadyOpen

        val collateral = marketItem.currentPrice * quantity * config.shortCollateralRatio
        val withdraw   = wallet.withdraw(player, collateral)
        if (withdraw is WithdrawResult.InsufficientFunds)
            return ShortResult.InsufficientCollateral

        val position = ShortPosition(
            playerUuid = player.uniqueId,
            item       = item,
            quantity   = quantity,
            entryPrice = marketItem.currentPrice,
            collateral = collateral
        )
        portfolio.saveShortPosition(position)

        return ShortResult.Opened(
            collateral = collateral,
            entryPrice = marketItem.currentPrice
        )
    }

    override fun closeShort(player: Player, item: ItemKey): ShortResult {
        val position = portfolio.getShortPositions(player.uniqueId)[item]
            ?: return ShortResult.NoPosition

        val marketItem = prices.getItem(item) ?: return ShortResult.NotListed
        val pnl        = position.unrealisedPnl(marketItem.currentPrice)
        val settlement = position.collateral + pnl

        wallet.deposit(player, settlement)
        portfolio.removeShortPosition(player.uniqueId, item)

        portfolio.addPnl(player.uniqueId, java.math.BigDecimal.valueOf(pnl))

        return ShortResult.Closed(pnl = pnl)
    }

    // ── Internal: process triggered limit orders ───────────────────────────────
    // NOT in public TradeService interface — called only by MarketTickTask

    /**
     * Check all open orders for [item] at [currentPrice] and execute any
     * that are triggered. Called from the market tick on the main thread.
     */
    fun processTriggeredOrders(item: ItemKey, currentPrice: Double) {
        val triggered = orders.getTriggeredOrders(item, currentPrice)
        for (order in triggered) {
            val player = org.bukkit.Bukkit.getPlayer(order.playerUUID) ?: continue
            val result = if (order.isBuyOrder)
                executeBuy(player, item, order.quantity)
            else
                executeSell(player, item, order.quantity)

            if (result is TradeResult.Success) {
                orders.markFilled(order.orderId)
            }
        }
        // Prune expired orders
        orders.pruneExpired(item)
    }
}
