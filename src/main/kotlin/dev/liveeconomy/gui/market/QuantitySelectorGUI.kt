package dev.liveeconomy.gui.market

import dev.liveeconomy.api.economy.TradeService
import dev.liveeconomy.api.economy.result.TradeResult
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.data.model.MarketItem
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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Buy/sell quantity selector — confirms trades before execution.
 * No [plugin.xxx] references.
 */
class QuantitySelectorGUI(
    private val trade:       TradeService,
    private val wallet:      WalletService,
    private val mapper:      ItemKeyMapper,
    private val menuManager: MenuManager,
    private val symbol:      String
) {
    data class SelectorState(
        val item:   MarketItem,
        val isBuy:  Boolean,
        var qty:    Int
    )

    private val states = ConcurrentHashMap<UUID, SelectorState>()

    // Slot layout
    private val SLOT_PREVIEW  = 13
    private val SLOT_MINUS_64 = 9;  private val SLOT_MINUS_16 = 10; private val SLOT_MINUS_1 = 11
    private val SLOT_PLUS_1   = 15; private val SLOT_PLUS_16  = 16; private val SLOT_PLUS_64 = 17
    private val PRICE_STRIP   = intArrayOf(19, 20, 21, 22, 23, 24, 25)
    private val SLOT_CANCEL   = 29; private val SLOT_SUMMARY = 31; private val SLOT_CONFIRM = 33
    private val SLOT_TOGGLE   = 46

    fun open(player: Player, item: MarketItem, isBuy: Boolean, qty: Int = 1) {
        states[player.uniqueId] = SelectorState(item, isBuy, qty.coerceAtLeast(1))
        menuManager.openAndStack(player, buildMenu(player, states[player.uniqueId]!!))
    }

    private fun buildMenu(player: Player, state: SelectorState): LiveMenu {
        val item    = state.item
        val price   = if (state.isBuy) item.askPrice else item.bidPrice
        val total   = price * state.qty
        val balance = wallet.getBalance(player)
        val canAff  = !state.isBuy || balance >= total
        val material = mapper.toMaterial(item.itemKey) ?: Material.PAPER

        val modeColor = if (state.isBuy) "§a" else "§c"
        val modeLabel = if (state.isBuy) "§a§lBUY MODE" else "§c§lSELL MODE"

        val menu = LiveMenu(
            title = "§0§l» $modeColor§l${item.itemKey.displayName()} — ${if (state.isBuy) "Buy" else "Sell"}",
            rows  = 6
        )
        for (i in 0 until 54) menu.setItem(i, border())

        // ── Item preview ──────────────────────────────────────────
        menu.setItem(SLOT_PREVIEW, itemStack(material) {
            name("$modeColor§l${item.itemKey.displayName()}")
            lore(Theme.SEP,
                "§7Quantity:    §e×${state.qty}",
                "§7Unit price:  §f$symbol${MoneyFormat.full(price)}",
                "§7Total:       ${if (canAff) "§a" else "§c"}$symbol${MoneyFormat.full(total)}",
                Theme.SEP)
        })

        // ── Quantity buttons ──────────────────────────────────────
        fun qtyBtn(mat: Material, label: String, delta: Int) = itemStack(mat) {
            name(label); lore("§7Click to adjust by $delta")
        }
        menu.setSlot(SLOT_MINUS_64, qtyBtn(Material.RED_STAINED_GLASS_PANE, "§c-64", -64)) { p -> adjust(p, state, -64) }
        menu.setSlot(SLOT_MINUS_16, qtyBtn(Material.RED_STAINED_GLASS_PANE, "§c-16", -16)) { p -> adjust(p, state, -16) }
        menu.setSlot(SLOT_MINUS_1,  qtyBtn(Material.RED_STAINED_GLASS_PANE, "§c-1",  -1))  { p -> adjust(p, state, -1) }
        menu.setSlot(SLOT_PLUS_1,   qtyBtn(Material.LIME_STAINED_GLASS_PANE, "§a+1",  1))   { p -> adjust(p, state, 1) }
        menu.setSlot(SLOT_PLUS_16,  qtyBtn(Material.LIME_STAINED_GLASS_PANE, "§a+16", 16))  { p -> adjust(p, state, 16) }
        menu.setSlot(SLOT_PLUS_64,  qtyBtn(Material.LIME_STAINED_GLASS_PANE, "§a+64", 64))  { p -> adjust(p, state, 64) }

        // ── Price strip ───────────────────────────────────────────
        val stripMat = if (canAff) Material.LIME_STAINED_GLASS_PANE else Material.RED_STAINED_GLASS_PANE
        PRICE_STRIP.forEach { menu.setItem(it, itemStack(stripMat) { name("§r") }) }
        menu.setItem(PRICE_STRIP[3], itemStack(Material.GOLD_INGOT) {
            name("§f§lPrice Breakdown")
            lore(Theme.SEP,
                "§7Unit price  §f$symbol${MoneyFormat.full(price)}",
                "§7Quantity    §e×${state.qty}",
                "§7Total       ${if (canAff) "§a" else "§c"}$symbol${MoneyFormat.full(total)}",
                "§7Balance     §6$symbol${MoneyFormat.full(balance)}",
                Theme.SEP); hideAll()
        })

        // ── Action buttons ────────────────────────────────────────
        menu.setSlot(SLOT_CANCEL, itemStack(Material.BARRIER) { name("§c§lCancel") }) { p ->
            SoundUtil.play(p, Sound.UI_BUTTON_CLICK); menuManager.kickBack(p)
        }

        menu.setItem(SLOT_SUMMARY, itemStack(Material.PAPER) {
            name("§f§lSummary")
            lore(Theme.SEP, "$modeLabel", "§7×${state.qty} ${item.itemKey.displayName()}",
                "§7Total: ${if (canAff) "§a" else "§c"}$symbol${MoneyFormat.full(total)}", Theme.SEP)
        })

        val confirmMat = if (canAff) Material.LIME_CONCRETE else Material.RED_CONCRETE
        menu.setSlot(SLOT_CONFIRM, itemStack(confirmMat) {
            name(if (canAff) "§a§lConfirm ${if (state.isBuy) "Buy" else "Sell"}" else "§c§lInsufficient Funds")
            lore(if (!canAff) "§7Need §c$symbol${MoneyFormat.full(total - balance)} §7more" else "")
        }) { p ->
            if (!canAff) { SoundUtil.play(p, Sound.ENTITY_VILLAGER_NO); return@setSlot }
            val result = if (state.isBuy) trade.executeBuy(p, item.itemKey, state.qty)
                         else             trade.executeSell(p, item.itemKey, state.qty)
            when (result) {
                is TradeResult.Success -> {
                    val msg = if (state.isBuy) "§aBought §f×${state.qty} ${item.itemKey.displayName()} §afor §f$symbol${MoneyFormat.full(result.total)}"
                              else             "§cSold §f×${state.qty} ${item.itemKey.displayName()} §afor §f$symbol${MoneyFormat.full(result.total)}"
                    p.sendMessage("${ChatUtil.prefix()}$msg")
                    if (state.isBuy) SoundUtil.play(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP)
                    else             SoundUtil.play(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, pitch = 0.8f)
                    menuManager.kickBack(p)
                }
                is TradeResult.InsufficientFunds  -> p.sendMessage("${ChatUtil.prefix()}§cInsufficient funds.")
                is TradeResult.InsufficientItems  -> p.sendMessage("${ChatUtil.prefix()}§cNot enough items to sell.")
                is TradeResult.NoInventorySpace   -> p.sendMessage("${ChatUtil.prefix()}§cInventory is full.")
                else -> p.sendMessage("${ChatUtil.prefix()}§cTrade failed.")
            }
        }

        // ── Mode toggle ───────────────────────────────────────────
        val toggleMat = if (state.isBuy) Material.LIME_DYE else Material.RED_DYE
        menu.setSlot(SLOT_TOGGLE, itemStack(toggleMat) {
            name("$modeColor§lMode: ${if (state.isBuy) "Buying" else "Selling"}")
            lore("§7Click to switch to ${if (state.isBuy) "§cSell" else "§aBuy"} mode")
        }) { p ->
            state.isBuy.let { /* flip */ }
            states[p.uniqueId] = state.copy(isBuy = !state.isBuy)
            menuManager.open(p, buildMenu(p, states[p.uniqueId]!!))
        }

        return menu
    }

    private fun adjust(player: Player, state: SelectorState, delta: Int) {
        state.qty = (state.qty + delta).coerceIn(1, 9999)
        menuManager.open(player, buildMenu(player, state))
    }

    private fun border() = itemStack(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }
}
