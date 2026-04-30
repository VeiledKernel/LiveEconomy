package dev.liveeconomy.gui.player

import dev.liveeconomy.api.economy.TradeService
import dev.liveeconomy.api.economy.result.ShortResult
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.data.model.TradeAction
import dev.liveeconomy.gui.framework.LiveMenu
import dev.liveeconomy.gui.framework.MenuManager
import dev.liveeconomy.gui.shared.ItemBuilder.itemStack
import dev.liveeconomy.gui.shared.Skulls
import dev.liveeconomy.gui.shared.Theme
import dev.liveeconomy.view.mapper.ViewMapper
import dev.liveeconomy.util.ChatUtil
import dev.liveeconomy.util.MoneyFormat
import dev.liveeconomy.util.SoundUtil
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Portfolio screen — renders a [PortfolioView], zero derived state.
 *
 * All computation (price lookups, pnl sign, size counts) lives in
 * [PortfolioViewBuilder]. This class only maps view model fields to slots.
 */
class PortfolioGUI(
    private val viewMapper:  ViewMapper,
    private val trade:       TradeService,
    private val mapper:      ItemKeyMapper,
    private val menuManager: MenuManager,
    private val symbol:      String
) {
    fun open(player: Player) {
        val v    = viewMapper.portfolio(player.uniqueId)
        val menu = LiveMenu("§0§l» §d§lPortfolio", rows = 6)
        for (i in 0 until 54) menu.setItem(i, border())

        // ── Summary ───────────────────────────────────────────────
        menu.setItem(4, itemStack(Material.GOLD_INGOT) {
            name("§6§lPortfolio Summary")
            lore(Theme.SEP,
                "§7Lifetime P&L  §6$symbol${MoneyFormat.full(v.pnl)}",
                "§7Win Rate      §f${String.format("%.1f", v.stats.winRate * 100)}%",
                "§7Total Trades  §f${v.stats.totalTrades}",
                "§7Volume        §f$symbol${MoneyFormat.compact(v.stats.totalVolume)}",
                Theme.SEP)
        })

        // ── Holdings — view already has prices ────────────────────
        v.holdings.entries.take(21).forEachIndexed { index, (item, entry) ->
            val material = mapper.toMaterial(item) ?: Material.PAPER
            menu.setItem(10 + index, itemStack(material) {
                name("§f§l${item.displayName()}")
                lore("§7Quantity: §e×${entry.quantity}",
                    "§7Price: §f$symbol${MoneyFormat.full(entry.currentPrice)}")
            })
        }

        // ── Shorts — view already has unrealised P&L ──────────────
        v.shorts.entries.take(7).forEachIndexed { index, (item, entry) ->
            val color    = if (entry.isPnlPositive) "§a" else "§c"
            val material = mapper.toMaterial(item) ?: Material.RED_STAINED_GLASS_PANE
            menu.setSlot(37 + index, itemStack(material) {
                name("§c§lShort: §f${item.displayName()}")
                lore("§7Qty:     §e×${entry.position.quantity}",
                    "§7Entry:   §f$symbol${MoneyFormat.full(entry.position.entryPrice)}",
                    "§7Current: §f$symbol${MoneyFormat.full(entry.currentPrice)}",
                    "§7P&L:     $color$symbol${MoneyFormat.full(entry.unrealisedPnl)}",
                    "§eClick to close position")
            }) { p ->
                when (val result = trade.closeShort(p, item)) {
                    is ShortResult.Closed -> {
                        val c = if (result.pnl >= 0) "§a" else "§c"
                        p.sendMessage("${ChatUtil.prefix()}§7Short closed. P&L: $c$symbol${MoneyFormat.full(result.pnl)}")
                        SoundUtil.play(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP)
                        open(p)
                    }
                    else -> p.sendMessage("${ChatUtil.prefix()}§cCould not close position.")
                }
            }
        }

        // ── Recent transactions ───────────────────────────────────
        val fmt = SimpleDateFormat("MM/dd HH:mm")
        v.recentTxs.forEachIndexed { index, tx ->
            val isBuy    = tx.action == TradeAction.BUY
            val actColor = if (isBuy) "§a" else "§c"
            val totalStr = if (isBuy) "§c-$symbol${MoneyFormat.full(tx.total)}"
                           else       "§a+$symbol${MoneyFormat.full(tx.total)}"
            val material = mapper.toMaterial(tx.item) ?: Material.PAPER
            menu.setItem(28 + index, itemStack(material) {
                name("$actColor§l${tx.item.displayName()}")
                lore(Theme.SEP, "§7Action  ${if (isBuy) "§aBUY" else "§cSELL"}",
                    "§7Qty     §e×${tx.quantity}",
                    "§7Unit    §f$symbol${MoneyFormat.full(tx.unitPrice)}",
                    "§7Total   $totalStr", Theme.SEP,
                    "§7${fmt.format(Date(tx.timestamp))}")
            })
        }

        menu.setItem(45, itemStack(Material.SPYGLASS) {
            name("§b§lTrader Stats")
            lore(Theme.SEP,
                "§7Win Rate  §f${String.format("%.1f", v.stats.winRate * 100)}%",
                "§7Avg ROI   §f${String.format("%.2f", v.stats.avgRoi)}%",
                "§7Buys      §f${v.stats.totalBuys}",
                "§7Sells     §f${v.stats.totalSells}", Theme.SEP)
        })

        menu.setSlot(49, itemStack(Material.BARRIER) { name("§c§lClose") }) { p ->
            menuManager.kickBack(p)
        }
        menuManager.open(player, menu)
    }

    private fun border() = itemStack(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }
}
