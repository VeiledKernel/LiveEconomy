package dev.liveeconomy.gui.market

import dev.liveeconomy.api.economy.MarketQueryService
import dev.liveeconomy.api.economy.PriceService
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.gui.framework.LiveMenu
import dev.liveeconomy.gui.framework.MenuManager
import dev.liveeconomy.gui.shared.ItemBuilder.itemStack
import dev.liveeconomy.gui.shared.Skulls
import dev.liveeconomy.gui.shared.Theme
import dev.liveeconomy.util.MoneyFormat
import dev.liveeconomy.util.SoundUtil
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

/** All-items browse screen. No plugin.xxx. */
class SearchGUI(
    private val query:       MarketQueryService,
    private val price:       PriceService,
    private val mapper:      ItemKeyMapper,
    private val menuManager: MenuManager,
    private val symbol:      String
) {
    fun open(player: Player) {
        val allItems = query.getAllItems().values.sortedBy { it.itemKey.displayName() }
        val menu     = LiveMenu("§0§l» §f§lSearch All Items", rows = 6)
        for (i in 0 until 54) menu.setItem(i, border())
        menu.setItem(4, itemStack(Material.COMPASS) {
            name("§f§lAll Market Items"); lore("§7\${allItems.size} items listed")
        })
        allItems.take(36).forEachIndexed { index, item ->
            val currentPrice = price.getPrice(item.itemKey) ?: item.basePrice
            val material     = mapper.toMaterial(item.itemKey) ?: Material.PAPER
            val changePct    = price.getPriceChangePercent(item.itemKey) ?: 0.0
            val changeColor  = if (changePct >= 0) "§a" else "§c"
            menu.setSlot(9 + index, itemStack(material) {
                name("§f§l\${item.itemKey.displayName()}")
                lore("§7Price: §f\$symbol\${MoneyFormat.full(currentPrice)}",
                    "§7Change: \$changeColor\${MoneyFormat.percent(changePct)}",
                    "§7Category: §7\${item.category.displayName}", "", "§eClick to view")
            }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK) }
        }
        menu.setSlot(49, Skulls.of(Skulls.BACK) { name("\${Theme.GRAY}◀ Back") }) { menuManager.kickBack(it) }
        menuManager.openAndStack(player, menu)
    }
    private fun border() = itemStack(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }
}
