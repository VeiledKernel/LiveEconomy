package dev.liveeconomy.gui.trading

import dev.liveeconomy.core.alert.AlertService
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

/**
 * Price alert screen — renders an [dev.liveeconomy.gui.viewmodel.AlertView], zero derived state.
 *
 * All computation (current prices, distance %, triggered state) lives in
 * [AlertViewBuilder]. This class only maps view model fields to slots.
 */
class PriceAlertGUI(
    private val viewMapper:   ViewMapper,
    private val alertService: AlertService,
    private val menuManager:  MenuManager,
    private val symbol:       String
) {
    private val alertSlots = (9..26).toList()

    fun open(player: Player) {
        val v    = viewMapper.alerts(player)
        val menu = LiveMenu("§0§l» §e§l🔔 Price Alerts", rows = 4)
        for (i in 0 until 36) menu.setItem(i, border())

        // ── Header ────────────────────────────────────────────────
        val headerColor = if (v.canAddMore) "§a" else "§c"
        menu.setItem(4, Skulls.of(Skulls.ALERT_BELL) {
            name("§e§lPrice Alerts")
            lore(Theme.SEP, "§7Active: $headerColor${v.alertCount} §7/ §f${v.limit}",
                "§7Alerts fire once, then auto-remove.", Theme.SEP,
                if (v.alerts.isEmpty()) "§7No active alerts." else "§7Click any alert to cancel.")
        })

        // ── Alert entries — view already has prices + distances ───
        v.alerts.forEachIndexed { index, entry ->
            if (index >= alertSlots.size) return@forEachIndexed
            val dirLabel = if (entry.isAbove) "§aABOVE" else "§cBELOW"
            val dirArrow = if (entry.isAbove) "§a▲" else "§c▼"
            val dirColor = if (entry.isAbove) "§a" else "§c"
            val distStr  = when {
                entry.distancePct <= 5.0  -> "§a${String.format("%.1f", entry.distancePct)}% away (very close!)"
                entry.distancePct <= 15.0 -> "§e${String.format("%.1f", entry.distancePct)}% away"
                else                      -> "§7${String.format("%.1f", entry.distancePct)}% away"
            }
            menu.setSlot(alertSlots[index], itemStack(Material.PAPER) {
                name("§f§l${entry.alert.item.displayName()}")
                lore(Theme.SEP, "§7Direction  $dirArrow $dirLabel",
                    "§7Target     §6$symbol${MoneyFormat.full(entry.alert.targetPrice)}",
                    "§7Current    $dirColor$symbol${MoneyFormat.full(entry.currentPrice)}",
                    Theme.SEP, if (entry.isTriggered) "§a⚡ Triggering next tick!" else distStr,
                    Theme.SEP, "§cClick to cancel this alert")
            }) { p ->
                alertService.removeAlert(p.uniqueId, entry.alert.item)
                SoundUtil.play(p, Sound.BLOCK_NOTE_BLOCK_BASS)
                p.sendMessage("${ChatUtil.prefix()}§7Alert cancelled: §f${entry.alert.item.displayName()} " +
                    "$dirColor$dirLabel §7$symbol${MoneyFormat.full(entry.alert.targetPrice)}")
                open(p)
            }
        }

        // Empty slots
        for (i in v.alertCount until minOf(alertSlots.size, v.limit)) {
            menu.setItem(alertSlots[i], itemStack(Material.GRAY_STAINED_GLASS_PANE) {
                name("§7Empty Slot"); lore("§7Use §e/invest alert <item> <price> <above|below>")
            })
        }

        // ── Add button ────────────────────────────────────────────
        menu.setSlot(29, itemStack(if (v.canAddMore) Material.LIME_STAINED_GLASS_PANE else Material.RED_STAINED_GLASS_PANE) {
            name(if (v.canAddMore) "§a§l+ Add Alert" else "§c§lAlert Limit Reached")
            lore(Theme.SEP,
                if (v.canAddMore) "§7Use: §e/invest alert <item> <price> <above|below>"
                else "§7Prestige to unlock more slots.", Theme.SEP)
        }) { p ->
            if (!v.canAddMore) p.sendMessage("${ChatUtil.prefix()}§cAlert limit reached (§e${v.limit}§c).")
            else p.sendMessage("${ChatUtil.prefix()}§7Use: §e/invest alert <item> <price> <above|below>")
        }

        menu.setSlot(31, Skulls.of(Skulls.BACK) { name("§7◀ Back") }) { p ->
            SoundUtil.play(p, Sound.UI_BUTTON_CLICK); menuManager.kickBack(p)
        }
        menuManager.openAndStack(player, menu)
    }

    private fun border() = itemStack(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }
}
