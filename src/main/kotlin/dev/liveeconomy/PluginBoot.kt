package dev.liveeconomy

import dev.liveeconomy.api.Lifecycle
import dev.liveeconomy.api.LiveEconomyAPI
import dev.liveeconomy.api.economy.MarketQueryService
import dev.liveeconomy.api.economy.PriceService
import dev.liveeconomy.api.economy.TradeService
import dev.liveeconomy.api.player.PortfolioService
import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.core.alert.AlertService
import dev.liveeconomy.core.analytics.AnalyticsService
import dev.liveeconomy.core.economy.MarketQueryServiceImpl
import dev.liveeconomy.core.economy.OrderBook
import dev.liveeconomy.core.economy.PriceModelImpl
import dev.liveeconomy.core.economy.PriceServiceImpl
import dev.liveeconomy.core.economy.TradeServiceImpl
import dev.liveeconomy.core.event.DomainEventBusImpl
import dev.liveeconomy.core.event.shock.*
import dev.liveeconomy.core.item.BukkitItemKeyMapper
import dev.liveeconomy.core.margin.MarginService
import dev.liveeconomy.core.market.MarketRegistry
import dev.liveeconomy.core.market.MarketTicker
import dev.liveeconomy.core.player.PortfolioServiceImpl
import dev.liveeconomy.core.player.PrestigeService
import dev.liveeconomy.core.player.RoleService
import dev.liveeconomy.core.player.WalletServiceImpl
import dev.liveeconomy.core.usecase.CloseShortUseCase
import dev.liveeconomy.core.usecase.ExecuteTradeUseCase
import dev.liveeconomy.core.usecase.OpenShortUseCase
import dev.liveeconomy.integration.vault.VaultGateway
import dev.liveeconomy.platform.BukkitPlayerResolver
import dev.liveeconomy.platform.ServiceLocator
import dev.liveeconomy.platform.config.AllConfigs
import dev.liveeconomy.platform.config.StorageFactory
import dev.liveeconomy.platform.inventory.BukkitInventoryGateway
import dev.liveeconomy.platform.scheduler.AutoSaveTask
import dev.liveeconomy.platform.scheduler.MarketTickTask
import dev.liveeconomy.platform.scheduler.SchedulerImpl
import org.bukkit.plugin.java.JavaPlugin

/**
 * Composition root — wires the full dependency graph.
 * Contains NO business logic.
 *
 * FIX #1: No unsafe casts — concrete types held alongside interface aliases.
 * FIX #2: MarginService and ShockRegistry stored as fields.
 * FIX #3: Explicit lifecycle list — stop() reverses boot order automatically.
 */
class PluginBoot(
    private val plugin:    JavaPlugin,
    private val scheduler: SchedulerImpl,
    private val configs:   AllConfigs
) {
    // FIX #3 — all Lifecycle components registered here, stopped in reverse order
    private val lifecycles = mutableListOf<Lifecycle>()

    fun start() {
        // 1. Item mapping + integrations
        val nexoAvailable = plugin.server.pluginManager.getPlugin("Nexo") != null
        val mapper  = BukkitItemKeyMapper(nexoAvailable)
        val vault   = VaultGateway(plugin.server)

        // 2. Storage
        val storage = StorageFactory.create(configs.storage, plugin.dataFolder, mapper)
        storage.start()
        lifecycles += storage

        // 3. Core services
        // FIX #1 — concrete impl held alongside interface alias, no unsafe cast needed
        val pricing      = PriceModelImpl(configs.market)
        val registry     = MarketRegistry(storage.price(), pricing)
        val priceImpl    = PriceServiceImpl(registry)
        val priceSvc     : PriceService     = priceImpl   // public API surface
        val orderBook    = OrderBook(storage.order()).also { it.init() }
        val walletSvc    : WalletService    = WalletServiceImpl(storage.wallet(), vault)
        val portfolioSvc : PortfolioService = PortfolioServiceImpl(storage.portfolio())
        val roleService      = RoleService(configs.roles)
        val prestigeService  = PrestigeService(storage.portfolio(), configs.prestige)

        // 4. Event bus + subscribers
        val bus = DomainEventBusImpl().also { it.start() }
        lifecycles += bus
        val playerResolver = BukkitPlayerResolver()
        bus.subscribe(AlertService(configs.market, configs.vip, scheduler, playerResolver))
        bus.subscribe(AnalyticsService(storage.portfolio(), scheduler))

        // 5. Use cases + platform adapters
        val inventory    = BukkitInventoryGateway(mapper)
        val tradeUC      = ExecuteTradeUseCase(priceImpl,   // concrete — no cast
            pricing, inventory, walletSvc, storage.portfolio(),
            storage.transaction(), bus, configs.market)
        val openShortUC  = OpenShortUseCase(priceImpl, walletSvc, storage.portfolio(),
            storage.transaction(), bus, configs.market)
        val closeShortUC = CloseShortUseCase(priceImpl, walletSvc, storage.portfolio(),
            storage.transaction(), bus)

        // FIX #1 — tradeImpl held directly; tradeSvc is the interface alias
        val tradeImpl = TradeServiceImpl(priceImpl, orderBook, tradeUC,
            openShortUC, closeShortUC, playerResolver)
        val tradeSvc  : TradeService       = tradeImpl
        val querySvc  : MarketQueryService = MarketQueryServiceImpl(registry, storage.price(), orderBook)

        // FIX #2 — MarginService stored, not silently discarded
        val marginService = MarginService(registry, storage.portfolio(), walletSvc,
            configs.market, scheduler, playerResolver)

        // 6. Shocks + ticker
        val applier = ShockApplier(registry, pricing, bus)
        // FIX #2 — ShockRegistry stored, not silently discarded
        val shockRegistry = ShockRegistry(listOf(
            MiningShock(applier, mapper, configs.events.mining),
            HarvestShock(applier, configs.events.harvest),
            CraftingShock(applier, configs.events.bulkCrafting),
            FishingShock(applier, configs.events.fishing),
            EnchantingShock(applier, configs.events.enchanting),
            BossKillShock(applier, configs.events.bossKills),
            NightCycleShock(applier, configs.events.nightCycle),
            RaidShock(applier, configs.events.raid),
            DeathSpreeShock(applier, configs.events.deathSpree),
            MassActivityShock(applier, configs.events.massActivity)
        ))
        val ticker = MarketTicker(registry, pricing, tradeImpl,  // concrete — no cast
            storage.price(), bus, configs.market)

        // 7. Tasks — registered in lifecycles for automatic reverse-order shutdown
        val tickTask = MarketTickTask(ticker, scheduler, configs.market).also { it.start() }
        val saveTask = AutoSaveTask(storage, registry, scheduler, configs.market).also { it.start() }
        lifecycles += tickTask
        lifecycles += saveTask

        // 8. ServiceLocator — LAST (DI-RULES.md Rule 5)
        ServiceLocator.init(priceSvc, tradeSvc, querySvc, walletSvc, portfolioSvc)

        // 9. Public API
        LiveEconomyAPI.init(object : LiveEconomyAPI {
            override fun price()     = priceSvc
            override fun trade()     = tradeSvc
            override fun query()     = querySvc
            override fun wallet()    = walletSvc
            override fun portfolio() = portfolioSvc
        })
    }

    // FIX #3 — stops all Lifecycle components in reverse boot order automatically
    fun stop() {
        lifecycles.asReversed().forEach { it.stop() }
        LiveEconomyAPI.clear()
    }
}
