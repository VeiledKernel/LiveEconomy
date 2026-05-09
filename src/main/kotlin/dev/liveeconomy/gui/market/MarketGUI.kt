package dev.liveeconomy.gui.market

import dev.liveeconomy.api.economy.MarketQueryService
import dev.liveeconomy.api.economy.PriceService
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.core.alert.AlertService
import dev.liveeconomy.data.config.GuiConfig
import dev.liveeconomy.data.model.MarketItem
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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Main market browsing screen — category tabs, paginated items, nav bar.
 *
 * No [plugin.xxx] references. Layout driven by [GuiConfig].
 * Item cache built from [MarketQueryService] and refreshed on demand.
 */
class MarketGUI(
    private val query:       MarketQueryService,
    private val price:       PriceService,
    private val wallet:      WalletService,
    private val alertService: AlertService,
    private val mapper:      ItemKeyMapper,
    private val config:      GuiConfig,
    private val menuManager: MenuManager,
    private val symbol:      String,
    // Navigation lambdas — injected to avoid circular dependencies
    private val openSearch:     (Player) -> Unit,
    private val openAlerts:     (Player) -> Unit,
    private val openWallet:     (Player) -> Unit,
    private val openPortfolio:  (Player) -> Unit,
    private val openOrderBook:  (Player) -> Unit,
    private val openLeaderboard: (Player) -> Unit,
    private val openQuantity:   (Player, MarketItem, Boolean) -> Unit
) {
    private val playerCategory = ConcurrentHashMap<UUID, String>()
    private val playerPage     = ConcurrentHashMap<UUID, Int>()

    // Item cache keyed by category id
    private var itemsByCategory: Map<String, List<MarketItem>> = emptyMap()

    fun open(player: Player) {
        rebuildCache()
        val categories = itemsByCategory.keys.toList().sorted()
        if (categories.isEmpty()) {
            player.sendMessage("§c[Market] No categories loaded. Check categories/ and reload.")
            return
        }
        val cat = playerCategory.getOrDefault(player.uniqueId, categories.first())
        openCategory(player, cat)
    }

    fun openCategory(player: Player, categoryId: String) {
        playerCategory[player.uniqueId] = categoryId
        playerPage.putIfAbsent(player.uniqueId, 0)
        menuManager.open(player, buildMenu(player, categoryId))
    }

    fun clearState(player: Player) {
        playerCategory.remove(player.uniqueId)
        playerPage.remove(player.uniqueId)
    }

    fun rebuildCache() {
        itemsByCategory = query.getAllItems().values
            .groupBy { it.category.id }
            .mapValues { (_, v) -> v.sortedBy { it.itemKey.displayName() } }
    }

    // ── Menu builder ──────────────────────────────────────────────────────────

    private fun buildMenu(player: Player, categoryId: String): LiveMenu {
        val page      = playerPage.getOrDefault(player.uniqueId, 0)
        val items     = itemsByCategory[categoryId] ?: emptyList()
        val maxPage   = ((items.size - 1) / config.itemSlots.size.coerceAtLeast(1)).coerceAtLeast(0)
        val catName   = itemsByCategory.keys.firstOrNull { it == categoryId } ?: categoryId
        val balance   = wallet.getBalance(player)
        val index     = price.getIndex()
        val alertCount = alertService.getAlerts(player.uniqueId).size

        val border = itemStack(
            Material.matchMaterial(config.borderMaterialName) ?: Material.BLACK_STAINED_GLASS_PANE
        ) { name("${Theme.RESET}") }
        val filler = itemStack(
            Material.matchMaterial(config.fillerMaterialName) ?: Material.GRAY_STAINED_GLASS_PANE
        ) { name("${Theme.RESET}") }

        val menu = LiveMenu(title = config.resolveTitle(catName), rows = config.rows)
        config.borderSlots.forEach { menu.setItem(it, border) }

        // ── Market items ──────────────────────────────────────────
        val pageItems = items.drop(page * config.itemSlots.size).take(config.itemSlots.size)
        pageItems.forEachIndexed { i, item ->
            if (i >= config.itemSlots.size) return@forEachIndexed
            val currentPrice = price.getPrice(item.itemKey) ?: item.basePrice
            val changePct    = price.getPriceChangePercent(item.itemKey) ?: 0.0
            val changeColor  = if (changePct >= 0) "§a" else "§c"
            val material     = mapper.toMaterial(item.itemKey) ?: Material.PAPER

            menu.setSlot(config.itemSlots[i], itemStack(material) {
                name("§f§l${item.itemKey.displayName()}")
                lore("§7Price:  §f$symbol${MoneyFormat.full(currentPrice)}",
                    "§7Change: $changeColor${MoneyFormat.percent(changePct)}",
                    "", "§eLeft-click to buy  §7|  §eRight-click to sell")
            }) { p, isRight ->
                SoundUtil.play(p, Sound.UI_BUTTON_CLICK)
                openQuantity(p, item, !isRight)
            }
        }
        for (i in pageItems.size until config.itemSlots.size) {
            if (i < config.itemSlots.size) menu.setItem(config.itemSlots[i], filler)
        }

        // ── Pagination ────────────────────────────────────────────
        if (config.buttons.prevPage >= 0) {
            if (page > 0) {
                menu.setSlot(config.buttons.prevPage, Skulls.of(Skulls.ARROW_LEFT) {
                    name("${Theme.WHITE}${Theme.BOLD}◀  Previous")
                    lore("${Theme.GRAY}Page $page of ${maxPage + 1}", "${Theme.YELLOW}Click to go back")
                }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); playerPage[p.uniqueId] = page - 1; menuManager.open(p, buildMenu(p, categoryId)) }
            } else {
                menu.setItem(config.buttons.prevPage, Skulls.of(Skulls.ARROW_LEFT) {
                    name("${Theme.GRAY}◀  Previous"); lore("${Theme.GRAY}First page")
                })
            }
        }
        if (config.buttons.nextPage >= 0) {
            if (page < maxPage) {
                menu.setSlot(config.buttons.nextPage, Skulls.of(Skulls.ARROW_RIGHT) {
                    name("${Theme.WHITE}${Theme.BOLD}Next  ▶")
                    lore("${Theme.GRAY}Page ${page + 2} of ${maxPage + 1}", "${Theme.YELLOW}Click to continue")
                }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); playerPage[p.uniqueId] = page + 1; menuManager.open(p, buildMenu(p, categoryId)) }
            } else {
                menu.setItem(config.buttons.nextPage, Skulls.of(Skulls.ARROW_RIGHT) {
                    name("${Theme.GRAY}Next  ▶"); lore("${Theme.GRAY}Last page")
                })
            }
        }

        // ── Header buttons ────────────────────────────────────────
        if (config.buttons.search >= 0) {
            menu.setSlot(config.buttons.search, Skulls.of(Skulls.MAGNIFIER) {
                name("${Theme.WHITE}${Theme.BOLD}Search")
                lore("${Theme.GRAY}Browse all items", "${Theme.YELLOW}Click to open")
            }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); openSearch(p) }
        }

        if (config.buttons.title >= 0) {
            menu.setItem(config.buttons.title, itemStack(Material.NETHER_STAR) {
                name("${Theme.AQUA}${Theme.BOLD}$catName")
                lore(Theme.SEP, "${Theme.GRAY}Items  ${Theme.WHITE}${items.size}",
                    "${Theme.GRAY}Page   ${Theme.WHITE}${page + 1} / ${maxPage + 1}", Theme.SEP)
                glow(); hideAll()
            })
        }

        if (config.buttons.alerts >= 0) {
            val alertName = if (alertCount > 0) "${Theme.YELLOW}${Theme.BOLD}🔔  Alerts  ${Theme.GOLD}[$alertCount]"
                            else "${Theme.GRAY}🔔  Alerts"
            menu.setSlot(config.buttons.alerts, Skulls.of(Skulls.ALERT_BELL) {
                name(alertName)
                lore("${Theme.GRAY}Active  ${Theme.YELLOW}$alertCount", "${Theme.YELLOW}Click to manage")
            }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); openAlerts(p) }
        }

        if (config.buttons.wallet >= 0) {
            menu.setSlot(config.buttons.wallet, itemStack(Material.PLAYER_HEAD) {
                name("${Theme.YELLOW}${Theme.BOLD}${player.name}")
                lore(Theme.SEP, "${Theme.GRAY}Balance  ${Theme.price(symbol, balance)}",
                    Theme.SEP, "${Theme.YELLOW}Click to open wallet")
                meta { (it as? org.bukkit.inventory.meta.SkullMeta)?.owningPlayer = player }
            }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); openWallet(p) }
        }

        if (config.buttons.portfolio >= 0) {
            menu.setSlot(config.buttons.portfolio, itemStack(Material.CHEST) {
                name("${Theme.PURPLE}${Theme.BOLD}Portfolio")
                lore("${Theme.GRAY}Holdings, shorts & P&L", "${Theme.YELLOW}Click to open")
            }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); openPortfolio(p) }
        }

        if (config.buttons.orders >= 0) {
            menu.setSlot(config.buttons.orders, itemStack(Material.BOOK) {
                name("${Theme.AQUA}${Theme.BOLD}Order Book")
                lore("${Theme.GRAY}View & cancel limit orders", "${Theme.YELLOW}Click to open")
            }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); openOrderBook(p) }
        }

        if (config.buttons.index >= 0) {
            val indexColor = when { index >= 1100 -> Theme.GREEN; index >= 900 -> Theme.YELLOW; else -> Theme.RED }
            val trend = when { index >= 1100 -> "${Theme.GREEN}▲ Bull market"; index >= 900 -> "${Theme.YELLOW}→ Neutral"; else -> "${Theme.RED}▼ Bear market" }
            menu.setItem(config.buttons.index, itemStack(Material.COMPASS) {
                name("${Theme.GOLD}${Theme.BOLD}Market Index")
                lore(Theme.SEP, "${Theme.GRAY}Index  $indexColor${Theme.BOLD}${String.format("%.1f", index)}", trend, Theme.SEP); hideAll()
            })
        }

        if (config.buttons.leaderboard >= 0) {
            menu.setSlot(config.buttons.leaderboard, Skulls.of(Skulls.TROPHY) {
                name("${Theme.GOLD}${Theme.BOLD}Leaderboard")
                lore("${Theme.GRAY}Top traders by P&L", "${Theme.YELLOW}Click to open")
            }) { p -> SoundUtil.play(p, Sound.UI_BUTTON_CLICK); openLeaderboard(p) }
        }

        return menu
    }
}
