package dev.liveeconomy.gui.factory

import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.api.scheduler.Scheduler
import dev.liveeconomy.api.storage.TransactionStore
import dev.liveeconomy.core.alert.AlertService
import dev.liveeconomy.core.player.PrestigeService
import dev.liveeconomy.core.player.RoleService
import dev.liveeconomy.data.config.EconomyConfig
import dev.liveeconomy.data.config.GuiConfig
import dev.liveeconomy.data.config.PrestigeConfig
import dev.liveeconomy.data.model.MarketItem
import dev.liveeconomy.gui.EconomyFacade
import dev.liveeconomy.gui.framework.MenuManager
import dev.liveeconomy.gui.market.MarketGUI
import dev.liveeconomy.gui.market.QuantitySelectorGUI
import dev.liveeconomy.gui.market.SearchGUI
import dev.liveeconomy.gui.player.LeaderboardGUI
import dev.liveeconomy.gui.player.PortfolioGUI
import dev.liveeconomy.gui.player.RoleGUI
import dev.liveeconomy.gui.player.WalletGUI
import dev.liveeconomy.gui.trading.OrderBookGUI
import dev.liveeconomy.gui.trading.PriceAlertGUI
import org.bukkit.entity.Player

/**
 * Pure assembler — creates and wires all GUI screens.
 *
 * No business logic. No ServiceLocator. All dependencies injected.
 * Navigation between screens is done via lambda references so there
 * are no circular class dependencies between GUI files.
 *
 * DI Rule 4: factories are pure assemblers.
 * DI Rule 15: EconomyFacade ≤ 5 services — enforced here.
 */
class GuiFactory(
    private val economy:     EconomyFacade,
    private val mapper:      ItemKeyMapper,
    private val txStore:     TransactionStore,
    private val roles:       RoleService,
    private val prestige:    PrestigeService,
    private val alerts:      AlertService,
    private val scheduler:   Scheduler,
    private val economyCfg:  EconomyConfig,
    private val guiCfg:      GuiConfig,
    private val prestigeCfg: PrestigeConfig
) {
    // Shared listener — registered once as a Bukkit listener in PluginBoot
    val menuManager: MenuManager by lazy { MenuManager(scheduler) }

    private val sym: String get() = economyCfg.currencySymbol

    // ── Individual screen constructors ────────────────────────────────────────

    fun market(): MarketGUI = MarketGUI(
        query          = economy.query,
        price          = economy.price,
        wallet         = economy.wallet,
        alertService   = alerts,
        mapper         = mapper,
        config         = guiCfg,
        menuManager    = menuManager,
        symbol         = sym,
        openSearch     = { p -> search().open(p) },
        openAlerts     = { p -> priceAlerts().open(p) },
        openWallet     = { p -> wallet().open(p) },
        openPortfolio  = { p -> portfolio().open(p) },
        openOrderBook  = { p -> orderBook().open(p) },
        openLeaderboard = { p -> leaderboard().open(p) },
        openQuantity   = { p, item, isBuy -> quantity().open(p, item, isBuy) }
    )

    fun wallet(): WalletGUI = WalletGUI(
        wallet         = economy.wallet,
        portfolio      = economy.portfolio,
        price          = economy.price,
        query          = economy.query,
        roleService    = roles,
        prestige       = prestige,
        alertService   = alerts,
        prestigeCfg    = prestigeCfg,
        menuManager    = menuManager,
        symbol         = sym,
        openPortfolio  = { p -> portfolio().open(p) },
        openOrderBook  = { p -> orderBook().open(p) },
        openAlerts     = { p -> priceAlerts().open(p) },
        openLeaderboard = { p -> leaderboard().open(p) }
    )

    fun portfolio(): PortfolioGUI = PortfolioGUI(
        portfolio      = economy.portfolio,
        price          = economy.price,
        trade          = economy.trade,
        wallet         = economy.wallet,
        txStore        = txStore,
        mapper         = mapper,
        menuManager    = menuManager,
        symbol         = sym
    )

    fun orderBook(): OrderBookGUI = OrderBookGUI(
        query       = economy.query,
        trade       = economy.trade,
        menuManager = menuManager,
        symbol      = sym
    )

    fun leaderboard(): LeaderboardGUI = LeaderboardGUI(
        portfolio   = economy.portfolio,
        roleService = roles,
        menuManager = menuManager,
        symbol      = sym
    )

    fun search(): SearchGUI = SearchGUI(
        query       = economy.query,
        price       = economy.price,
        mapper      = mapper,
        menuManager = menuManager,
        symbol      = sym
    )

    fun priceAlerts(): PriceAlertGUI = PriceAlertGUI(
        alertService = alerts,
        price        = economy.price,
        prestige     = prestige,
        menuManager  = menuManager,
        symbol       = sym
    )

    fun role(): RoleGUI = RoleGUI(roles, menuManager)

    fun quantity(): QuantitySelectorGUI = QuantitySelectorGUI(
        trade       = economy.trade,
        wallet      = economy.wallet,
        mapper      = mapper,
        menuManager = menuManager,
        symbol      = sym
    )
}
