package dev.liveeconomy.gui.trading

import dev.liveeconomy.api.economy.MarketQueryService
import dev.liveeconomy.api.economy.TradeService
import dev.liveeconomy.api.economy.result.OrderResult
import dev.liveeconomy.gui.framework.LiveMenu
import dev.liveeconomy.gui.framework.MenuManager
import dev.liveeconomy.gui.shared.ItemBuilder.itemStack
import dev.liveeconomy.gui.shared.Skulls
import dev.liveeconomy.gui.shared.Theme
import dev.liveeconomy.util.ChatUtil
import dev.liveeconomy.util.MoneyFormat
import dev.liveeconomy.util.SoundUtil
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

/** Order book screen — all open limit orders, cancellation support. No plugin.xxx. */
class OrderBookGUI(
    private val query:       MarketQueryService,
    private val trade:       TradeService,
    private val menuManager: MenuManager,
    private val symbol:      String
) {
    fun open(player: Player) {
        val orders = query.getAllItems().keys.flatMap { query.getOpenOrders(it) }
        val menu   = LiveMenu("§0§l» §b§lOrder Book", rows = 6)
        for (i in 0 until 54) menu.setItem(i, border())
        menu.setItem(4, itemStack(Material.BOOK) {
            name("§b§lPending Orders")
            lore("§7\${orders.size} active limit orders", "§7Click your orders to cancel")
        })
        orders.take(36).forEachIndexed { index, order ->
            val isMine    = order.playerUUID == player.uniqueId
            val mat       = if (order.isBuyOrder) Material.LIME_STAINED_GLASS_PANE else Material.RED_STAINED_GLASS_PANE
            val orderItem = itemStack(mat) {
                name(if (order.isBuyOrder) "§aBuy Limit" else "§cSell Limit")
                lore("§7Player: §f\${order.playerName}", "§7Item: §f\${order.item.displayName()}",
                    "§7Qty: §e×\${order.quantity}", "§7Target: §6\$symbol\${MoneyFormat.full(order.targetPrice)}",
                    if (isMine) "§eClick to cancel" else "§8Not your order")
            }
            if (isMine) {
                menu.setSlot(9 + index, orderItem) { p ->
                    when (trade.cancelLimitOrder(p, order.orderId)) {
                        is OrderResult.Cancelled -> { p.sendMessage("\${ChatUtil.prefix()}§7Order cancelled.")
                            SoundUtil.play(p, Sound.BLOCK_NOTE_BLOCK_BASS); open(p) }
                        else -> p.sendMessage("\${ChatUtil.prefix()}§cCould not cancel.")
                    }
                }
            } else menu.setItem(9 + index, orderItem)
        }
        menu.setSlot(49, Skulls.of(Skulls.BACK) { name("\${Theme.GRAY}◀ Back") }) { menuManager.kickBack(it) }
        menuManager.openAndStack(player, menu)
    }
    private fun border() = itemStack(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }
}
