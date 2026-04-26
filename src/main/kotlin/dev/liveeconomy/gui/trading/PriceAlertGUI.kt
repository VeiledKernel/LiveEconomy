package dev.liveeconomy.gui.trading

import dev.liveeconomy.api.economy.PriceService
import dev.liveeconomy.core.alert.AlertService
import dev.liveeconomy.core.player.PrestigeService
import dev.liveeconomy.data.model.Direction
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

/**
 * Price alert management screen.
 * No [plugin.xxx] references.
 */
class PriceAlertGUI(
    private val alertService:  AlertService,
    private val price:         PriceService,
    private val prestige:      PrestigeService,
    private val menuManager:   MenuManager,
    private val symbol:        String
) {
    private val alertSlots = (9..26).toList()

    fun open(player: Player) {
        val alerts   = alertService.getAlerts(player.uniqueId)
        val limit    = alertService.getAlertLimit(player)
        val menu     = LiveMenu("§0§l» §e§l🔔 Price Alerts", rows = 4)

        for (i in 0 until 36) menu.setItem(i, border())

        // ── Header ────────────────────────────────────────────────
        val headerColor = if (alerts.size >= limit) "§c" else "§a"
        menu.setItem(4, Skulls.of(Skulls.ALERT_BELL) {
            name("§e§lPrice Alerts")
            lore(Theme.SEP, "§7Active: $headerColor${alerts.size} §7/ §f$limit",
                "§7Alerts fire once, then auto-remove.", Theme.SEP,
                if (alerts.isEmpty()) "§7No active alerts." else "§7Click any alert to cancel.")
        })

        // ── Alert slots ───────────────────────────────────────────
        alerts.forEachIndexed { index, alert ->
            if (index >= alertSlots.size) return@forEachIndexed
            val currentPrice = price.getPrice(alert.item) ?: 0.0
            val isAbove      = alert.direction == Direction.ABOVE
            val dirLabel     = if (isAbove) "§aABOVE" else "§cBELOW"
            val dirArrow     = if (isAbove) "§a▲" else "§c▼"
            val dirColor     = if (isAbove) "§a" else "§c"
            val triggered    = alert.isTriggered(currentPrice)
            val distance     = if (alert.targetPrice > 0)
                Math.abs((currentPrice - alert.targetPrice) / alert.targetPrice * 100) else 0.0
            val distStr = when {
                distance <= 5.0  -> "§a${String.format("%.1f", distance)}% away (very close!)"
                distance <= 15.0 -> "§e${String.format("%.1f", distance)}% away"
                else             -> "§7${String.format("%.1f", distance)}% away"
            }
            val material = dev.liveeconomy.api.item.ItemKeyMapper::class.java.let { Material.PAPER }

            menu.setSlot(alertSlots[index], itemStack(Material.PAPER) {
                name("§f§l${alert.item.displayName()}")
                lore(Theme.SEP, "§7Direction  $dirArrow $dirLabel",
                    "§7Target     §6$symbol${MoneyFormat.full(alert.targetPrice)}",
                    "§7Current    $dirColor$symbol${MoneyFormat.full(currentPrice)}",
                    Theme.SEP, if (triggered) "§a⚡ Triggering next tick!" else distStr,
                    Theme.SEP, "§cClick to cancel this alert")
            }) { p ->
                alertService.removeAlert(p.uniqueId, alert.item)
                SoundUtil.play(p, Sound.BLOCK_NOTE_BLOCK_BASS)
                p.sendMessage("${ChatUtil.prefix()}§7Alert cancelled: §f${alert.item.displayName()} " +
                    "$dirColor$dirLabel §7$symbol${MoneyFormat.full(alert.targetPrice)}")
                open(p)
            }
        }

        // Empty slots
        for (i in alerts.size until minOf(alertSlots.size, limit)) {
            menu.setItem(alertSlots[i], itemStack(Material.GRAY_STAINED_GLASS_PANE) {
                name("§7Empty Slot")
                lore("§7Use §e/invest alert <item> <price> <above|below>")
            })
        }

        // ── Add button ────────────────────────────────────────────
        val canAdd = alerts.size < limit
        menu.setSlot(29, itemStack(if (canAdd) Material.LIME_STAINED_GLASS_PANE else Material.RED_STAINED_GLASS_PANE) {
            name(if (canAdd) "§a§l+ Add Alert" else "§c§lAlert Limit Reached")
            lore(Theme.SEP,
                if (canAdd) "§7Use: §e/invest alert <item> <price> <above|below>"
                else "§7Prestige to unlock more slots.", Theme.SEP)
        }) { p ->
            if (!canAdd) p.sendMessage("${ChatUtil.prefix()}§cAlert limit reached (§e$limit§c).")
            else p.sendMessage("${ChatUtil.prefix()}§7Use: §e/invest alert <item> <price> <above|below>")
        }

        // ── Back ──────────────────────────────────────────────────
        menu.setSlot(31, Skulls.of(Skulls.BACK) { name("§7◀ Back") }) { p ->
            SoundUtil.play(p, Sound.UI_BUTTON_CLICK); menuManager.kickBack(p)
        }

        menuManager.openAndStack(player, menu)
    }

    private fun border() = itemStack(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }
}
