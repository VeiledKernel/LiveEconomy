package dev.liveeconomy

import dev.liveeconomy.api.LiveEconomyAPI
import dev.liveeconomy.api.economy.MarketQueryService
import dev.liveeconomy.api.economy.PriceService
import dev.liveeconomy.api.economy.TradeService
import dev.liveeconomy.api.event.DomainEventBus
import dev.liveeconomy.api.player.PortfolioService
import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.core.alert.AlertService
import dev.liveeconomy.core.analytics.AnalyticsService
import dev.liveeconomy.core.economy.*
import dev.liveeconomy.core.event.DomainEventBusImpl
import dev.liveeconomy.core.event.shock.*
import dev.liveeconomy.core.item.BukkitItemKeyMapper
import dev.liveeconomy.core.margin.MarginService
import dev.liveeconomy.core.market.MarketRegistry
import dev.liveeconomy.core.market.MarketTicker
import dev.liveeconomy.core.player.*
import dev.liveeconomy.core.usecase.*
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
 *
 * Contains NO business logic. If this exceeds 150 lines, split further.
 *
 * Boot order:
 *  1. Item mapping + integrations
 *  2. Storage
 *  3. Core services
 *  4. Event bus + subscribers
 *  5. Use cases + platform adapters
 *  6. Shocks + ticker
 *  7. Tasks (Lifecycle)
 *  8. ServiceLocator (LAST before API)
 *  9. Public API
 */
class PluginBoot(
    private val plugin:    JavaPlugin,
    private val scheduler: SchedulerImpl,
    private val configs:   AllConfigs
) {
    private lateinit var bus:      DomainEventBus
    private lateinit var tickTask: MarketTickTask
    private lateinit var saveTask: AutoSaveTask

    fun start() {
        // 1. Item mapping + integrations
        val nexoAvailable = plugin.server.pluginManager.getPlugin("Nexo") != null
        val mapper  = BukkitItemKeyMapper(nexoAvailable)
        val vault   = VaultGateway(plugin.server)

        // 2. Storage
        val storage = StorageFactory.create(configs.storage, plugin.dataFolder, mapper)
        storage.start()

        // 3. Core services
        val pricing     = PriceModelImpl(configs.market)
        val registry    = MarketRegistry(storage.price(), pricing)
        val priceSvc    : PriceService      = PriceServiceImpl(registry)
        val orderBook   = OrderBook(storage.order()).also { it.init() }
        val walletSvc   : WalletService     = WalletServiceImpl(storage.wallet(), vault)
        val portfolioSvc: PortfolioService  = PortfolioServiceImpl(storage.portfolio())
        val roleService     = RoleService(configs.roles)
        val prestigeService = PrestigeService(storage.portfolio(), configs.prestige)

        // 4. Event bus + subscribers
        bus = DomainEventBusImpl().also { it.start() }
        val playerResolver = BukkitPlayerResolver()
        bus.subscribe(AlertService(configs.market, configs.vip, scheduler, playerResolver))
        bus.subscribe(AnalyticsService(storage.portfolio(), scheduler))

        // 5. Use cases + platform adapters
        val inventory   = BukkitInventoryGateway(mapper)
        val tradeUC     = ExecuteTradeUseCase(priceSvc as PriceServiceImpl,
            pricing, inventory, walletSvc, storage.portfolio(), storage.transaction(), bus, configs.market)
        val openShortUC  = OpenShortUseCase(priceSvc, walletSvc, storage.portfolio(),
            storage.transaction(), bus, configs.market)
        val closeShortUC = CloseShortUseCase(priceSvc, walletSvc, storage.portfolio(),
            storage.transaction(), bus)
        val tradeSvc : TradeService          = TradeServiceImpl(priceSvc, orderBook, tradeUC,
            openShortUC, closeShortUC, playerResolver)
        val querySvc : MarketQueryService    = MarketQueryServiceImpl(registry, storage.price(), orderBook)
        MarginService(registry, storage.portfolio(), walletSvc, configs.market, scheduler, playerResolver)

        // 6. Shocks + ticker
        val applier = ShockApplier(registry, pricing, bus)
        ShockRegistry(listOf(
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
        val ticker = MarketTicker(registry, pricing, tradeSvc as TradeServiceImpl,
            storage.price(), bus, configs.market)

        // 7. Tasks
        tickTask = MarketTickTask(ticker, scheduler, configs.market).also { it.start() }
        saveTask = AutoSaveTask(storage, registry, scheduler, configs.market).also { it.start() }

        // 8. ServiceLocator — LAST (Rule 5)
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

    fun stop() {
        saveTask.stop()
        tickTask.stop()
        bus.stop()
    }
}
