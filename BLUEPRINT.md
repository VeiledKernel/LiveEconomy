# LiveEconomy v4.0 — Architecture Blueprint (v3, Final)

**Status:** Approved for execution  
**Companion:** DI-RULES.md v3

---

## Locked Decisions

| Decision | Choice |
|---|---|
| API stability | v4.0 = `@Experimental`. v4.1 = `@Stable`. Breaking changes → v5.0 |
| DI pattern | Constructor injection. See DI-RULES.md v3 — 11 rules |
| ServiceLocator | 3 permitted contexts only — forbidden everywhere else |
| Item identity | `ItemKey` interface + `ItemKeyMapper` — ALL conversions centralized |
| Player identity | `Player` directly (Bukkit-native) |
| Naming | `*ServiceImpl`, `*Store`, `*Gateway`, `*UseCase`, `*Facade` |
| Extension model | Strategy + Registry — closed for modification, open via extension |
| Internal events | `DomainEventBus : Lifecycle` — subscribe/unsubscribe/lifecycle |
| Storage transactions | MySQL/SQLite = real rollback. YAML = best-effort atomic write only. |
| Service grouping | `EconomyFacade` for GUI/command injection (not in core) |
| Interface granularity | Split at >5 methods. See Rule 10 in DI-RULES.md |

---

## Layer Dependency Graph

```
         ┌──────────────────────────────────────────┐
         │              api/                        │ ← Bukkit + nothing else
         │  (interfaces, events, ItemKey, results)  │   @Experimental until v4.1
         └──────────────────┬───────────────────────┘
                            │ implements / uses
         ┌──────────────────▼───────────────────────┐
         │              data/                       │ ← nothing
         │  (pure models, typed configs)            │
         └──────────────────┬───────────────────────┘
                            │ uses
         ┌──────────────────▼───────────────────────┐
         │              core/                       │ ← api/ + data/ only
         │  (service impls, use cases, domain logic)│   NO Bukkit, NO storage direct
         └───────┬──────────────────────┬───────────┘
                 │                      │
    ┌────────────▼──────┐  ┌────────────▼───────────┐
    │    storage/        │  │    integration/       │
    │  (store impls)     │  │  (vault, nexo, papi)  │
    └────────────────────┘  └───────────────────────┘
                 │
    ┌────────────▼──────────────────────────────────┐
    │           platform/                           │ ← ONLY layer with ServiceLocator
    │  (scheduler, config, listeners, Lifecycle.kt) │
    └───────┬───────────────────────────────────────┘
            │
    ┌───────▼───────────────────────────────────────┐
    │       gui/ + command/                         │ ← api/ + EconomyFacade via DI
    └───────────────────────────────────────────────┘
            │
    ┌───────▼───────────────────────────────────────┐
    │       LiveEconomy.kt                          │ ← composition root only (~100 lines)
    └───────────────────────────────────────────────┘
```

---

## Directory Structure

```
src/main/kotlin/dev/liveeconomy/
│
├── LiveEconomy.kt
│
├── api/
│   ├── LiveEconomyAPI.kt                 ← @Experimental — entry point
│   ├── item/
│   │   ├── ItemKey.kt                    ← interface: id, namespace, key
│   │   └── ItemKeyMapper.kt              ← interface: fromMaterial, toMaterial
│   ├── economy/
│   │   ├── PriceService.kt               ← getPrice, getBid, getAsk, getIndex
│   │   ├── TradeService.kt               ← executeBuy, executeSell
│   │   └── MarketQueryService.kt         ← getHistory, getStats, getItem
│   ├── player/
│   │   ├── WalletService.kt              ← getBalance, deposit, withdraw
│   │   └── PortfolioService.kt           ← getHoldings, getPnl, getStats
│   ├── storage/
│   │   ├── StorageProvider.kt            ← factory: wallet(), portfolio(), price(), tx()
│   │   ├── WalletStore.kt
│   │   ├── PortfolioStore.kt
│   │   ├── PriceStore.kt
│   │   └── TransactionStore.kt
│   ├── event/
│   │   ├── DomainEvent.kt                ← sealed interface
│   │   ├── DomainEventBus.kt             ← extends Lifecycle; publish/subscribe/unsubscribe
│   │   ├── DomainEventHandler.kt         ← fun handle(event: DomainEvent)
│   │   ├── TradeExecutedEvent.kt
│   │   ├── PriceChangedEvent.kt
│   │   └── ShockFiredEvent.kt
│   ├── scheduler/
│   │   └── Scheduler.kt                  ← runAsync, runOnMain, runRepeating, cancel
│   └── extension/
│       ├── PriceModifier.kt
│       ├── TradeHook.kt
│       └── ShockHandler.kt
│
├── data/
│   ├── model/
│   │   ├── MarketItem.kt
│   │   ├── TradeOrder.kt
│   │   ├── ShortPosition.kt
│   │   ├── PriceCandle.kt
│   │   ├── Transaction.kt
│   │   ├── Alert.kt
│   │   ├── PlayerStats.kt
│   │   └── MarketCategory.kt
│   └── config/
│       ├── EconomyConfig.kt
│       ├── MarketConfig.kt
│       ├── EventsConfig.kt
│       ├── StorageConfig.kt
│       ├── GuiConfig.kt
│       └── PrestigeConfig.kt
│
├── core/
│   ├── item/
│   │   ├── VanillaItemKey.kt             ← implements ItemKey for Material
│   │   ├── NexoItemKey.kt                ← implements ItemKey for Nexo
│   │   └── BukkitItemKeyMapper.kt        ← implements ItemKeyMapper; ALL conversions here
│   ├── economy/
│   │   ├── PriceServiceImpl.kt
│   │   ├── TradeServiceImpl.kt
│   │   ├── MarketQueryServiceImpl.kt
│   │   ├── PriceModelImpl.kt             ← holds List<PriceModifier>
│   │   └── OrderBook.kt
│   ├── player/
│   │   ├── WalletServiceImpl.kt
│   │   ├── PortfolioServiceImpl.kt
│   │   ├── RoleService.kt
│   │   └── PrestigeService.kt
│   ├── market/
│   │   ├── MarketTicker.kt
│   │   ├── PriceHistory.kt
│   │   ├── MarketIndex.kt
│   │   └── ShortService.kt
│   ├── usecase/
│   │   ├── ExecuteTradeUseCase.kt        ← market + wallet + portfolio + eventBus
│   │   ├── OpenShortUseCase.kt
│   │   └── CloseShortUseCase.kt
│   ├── event/
│   │   ├── DomainEventBusImpl.kt         ← implements DomainEventBus + Lifecycle
│   │   ├── BukkitEventBridge.kt          ← DomainEvent → Bukkit event
│   │   └── shock/
│   │       ├── ShockRegistry.kt          ← List<ShockHandler>
│   │       ├── MiningShock.kt
│   │       ├── HarvestShock.kt
│   │       ├── CraftingShock.kt
│   │       ├── FishingShock.kt
│   │       ├── EnchantingShock.kt
│   │       ├── BossKillShock.kt
│   │       ├── NightCycleShock.kt
│   │       ├── RaidShock.kt
│   │       ├── DeathSpreeShock.kt
│   │       └── MassActivityShock.kt
│   ├── alert/
│   │   ├── AlertService.kt
│   │   └── AlertChecker.kt               ← DomainEventHandler
│   ├── analytics/
│   │   └── AnalyticsService.kt           ← DomainEventHandler
│   ├── regional/
│   │   └── RegionalMarketService.kt
│   ├── margin/
│   │   └── MarginService.kt
│   └── license/
│       └── LicenseService.kt
│
├── storage/
│   ├── yaml/
│   │   ├── YamlStorageProvider.kt
│   │   ├── YamlWalletStore.kt            ← best-effort atomic write (no real rollback)
│   │   ├── YamlPortfolioStore.kt
│   │   ├── YamlPriceStore.kt
│   │   └── YamlTransactionStore.kt
│   └── sql/
│       ├── SqlStorageProvider.kt         ← abstract: real rollback via JDBC transactions
│       ├── SqlWalletStore.kt
│       ├── SqlPortfolioStore.kt
│       ├── SqlPriceStore.kt
│       ├── SqlTransactionStore.kt
│       ├── SqlTransactionScope.kt        ← real JDBC transaction boundary
│       ├── YamlTransactionScope.kt       ← no-op with logged warning
│       ├── sqlite/
│       │   └── SqliteStorageProvider.kt
│       └── mysql/
│           └── MysqlStorageProvider.kt
│
├── gui/
│   ├── framework/
│   │   ├── LiveMenu.kt
│   │   ├── MenuManager.kt
│   │   ├── MenuSession.kt
│   │   └── ClickHandler.kt
│   ├── market/
│   │   ├── MarketGUI.kt                  ← EconomyFacade + GuiConfig
│   │   ├── SearchGUI.kt
│   │   ├── MarketItemSlot.kt
│   │   └── quantity/
│   │       ├── QuantitySelectorGUI.kt
│   │       └── QuantityState.kt
│   ├── player/
│   │   ├── WalletGUI.kt
│   │   ├── PortfolioGUI.kt
│   │   ├── RoleGUI.kt
│   │   └── LeaderboardGUI.kt
│   ├── trading/
│   │   ├── OrderBookGUI.kt
│   │   └── PriceAlertGUI.kt
│   ├── factory/
│   │   └── GuiFactory.kt                 ← EconomyFacade → creates GUIs
│   └── shared/
│       ├── Theme.kt
│       ├── Skulls.kt
│       ├── ItemBuilder.kt
│       └── Components.kt
│
├── command/
│   ├── framework/
│   │   ├── CommandNode.kt
│   │   ├── SubCommand.kt
│   │   └── TabCompleter.kt
│   ├── market/
│   │   ├── MarketCommand.kt              ← EconomyFacade + GuiFactory
│   │   └── sub/
│   │       ├── HistorySubCommand.kt
│   │       ├── StatsSubCommand.kt
│   │       ├── AlertSubCommand.kt
│   │       ├── TransactionsSubCommand.kt
│   │       └── RoleSubCommand.kt
│   ├── admin/
│   │   ├── AdminCommand.kt
│   │   └── sub/
│   │       ├── ReloadSubCommand.kt
│   │       ├── CrashSubCommand.kt
│   │       ├── BullSubCommand.kt
│   │       ├── ShockSubCommand.kt
│   │       ├── SetPriceSubCommand.kt
│   │       ├── SaveSubCommand.kt
│   │       └── PrestigeSubCommand.kt
│   └── player/
│       ├── WalletCommand.kt
│       ├── PortfolioCommand.kt
│       ├── InvestCommand.kt
│       └── ShortCommand.kt
│
├── integration/
│   ├── vault/
│   │   ├── EconomyGateway.kt             ← interface abstraction over Vault
│   │   └── VaultGateway.kt               ← implements EconomyGateway
│   ├── nexo/
│   │   └── NexoIntegration.kt            ← implements Lifecycle
│   ├── papi/
│   │   └── PlaceholderExpansion.kt       ← ServiceLocator permitted (PAPI reflection)
│   └── essentials/
│       └── EssentialsSellBlocker.kt
│
├── platform/
│   ├── Lifecycle.kt                      ← interface: start(), stop()
│   ├── ServiceLocator.kt                 ← populated last in onEnable()
│   ├── scheduler/
│   │   ├── SchedulerImpl.kt              ← implements api/scheduler/Scheduler
│   │   ├── AsyncTradeQueue.kt            ← implements Lifecycle
│   │   ├── MarketTickTask.kt             ← implements Lifecycle
│   │   ├── AutoSaveTask.kt               ← implements Lifecycle
│   │   └── PriceDecayTask.kt             ← implements Lifecycle
│   ├── config/
│   │   ├── ConfigLoader.kt
│   │   ├── GuiLayoutLoader.kt
│   │   ├── CategoryLoader.kt
│   │   └── LangLoader.kt
│   └── listener/
│       ├── ListenerRegistry.kt
│       ├── PlayerListener.kt
│       ├── ShockListener.kt
│       └── TradeListener.kt
│
└── util/
    ├── ChatUtil.kt
    ├── SoundUtil.kt
    ├── InventoryUtil.kt
    └── MoneyFormat.kt
```

---

## Key Design Decisions Explained

### ItemKey + ItemKeyMapper (Risk 1 fix)

Every item identity conversion goes through one place:

```kotlin
// api/item/ItemKey.kt
interface ItemKey {
    val id: String        // "minecraft:diamond", "nexo:ruby"
    val namespace: String // "minecraft", "nexo"
    val key: String       // "diamond", "ruby"
}

// api/item/ItemKeyMapper.kt
interface ItemKeyMapper {
    fun fromMaterial(material: Material): ItemKey
    fun toMaterial(key: ItemKey): Material?
    fun fromNexoId(nexoId: String): ItemKey
}

// core/item/BukkitItemKeyMapper.kt — the ONLY place that touches Material directly
class BukkitItemKeyMapper(
    private val nexo: NexoIntegration  // for Nexo items
) : ItemKeyMapper {
    override fun fromMaterial(material: Material): ItemKey =
        VanillaItemKey("minecraft", material.key.key)
    override fun toMaterial(key: ItemKey): Material? =
        if (key.namespace == "minecraft") Material.matchMaterial(key.key) else null
    override fun fromNexoId(nexoId: String): ItemKey =
        NexoItemKey("nexo", nexoId)
}
```

**Rule:** No code outside `core/item/` and `platform/` ever references `Material` in a context where an `ItemKey` is expected.

---

### DomainEventBus as Lifecycle (Risk 2 fix)

```kotlin
// api/event/DomainEventBus.kt
interface DomainEventBus : Lifecycle {
    fun publish(event: DomainEvent)
    fun subscribe(handler: DomainEventHandler)
    fun unsubscribe(handler: DomainEventHandler)  // prevents memory leaks
}
```

**Flow:**
```
TradeExecutor.executeBuy()
    → domainBus.publish(TradeExecutedEvent)
        → AlertChecker.handle()     → fire alert if threshold hit
        → AnalyticsService.handle() → record trade stats
        → BukkitEventBridge.handle()→ fire Bukkit TradeExecutedEvent for external plugins
```

All subscribers registered at startup, unregistered on `domainBus.stop()`.

---

### TransactionScope Behavior by Backend (Risk 3 fix)

```kotlin
// storage/sql/SqlTransactionScope.kt
class SqlTransactionScope(private val conn: Connection) : TransactionScope {
    override fun <T> execute(block: () -> T): T {
        conn.autoCommit = false
        return try {
            val result = block()
            conn.commit()
            result
        } catch (e: Exception) {
            conn.rollback()   // real rollback
            throw e
        } finally {
            conn.autoCommit = true
        }
    }
}

// storage/yaml/YamlTransactionScope.kt
class YamlTransactionScope : TransactionScope {
    override fun <T> execute(block: () -> T): T {
        // YAML has no real transaction support.
        // This is best-effort: operations execute sequentially.
        // Partial writes are possible on crash — use SQL backend for production.
        plugin.logger.fine("[YamlTransactionScope] No-op transaction boundary.")
        return block()
    }
}
```

**Documented contract:**

| Backend | Transaction | Crash safety |
|---|---|---|
| MySQL | Real JDBC rollback | ✅ ACID |
| SQLite | Real JDBC rollback | ✅ ACID |
| YAML | Best-effort sequential | ⚠️ Partial writes possible |

---

### EconomyFacade (Risk 4 fix)

Prevents constructor explosion in GUI and command classes:

```kotlin
// Used in gui/ and command/ only — forbidden in core/
data class EconomyFacade(
    val price:     PriceService,
    val trade:     TradeService,
    val wallet:    WalletService,
    val portfolio: PortfolioService,
    val query:     MarketQueryService
)

// GUI receives one thing, not 5+
class MarketGUI(
    private val economy: EconomyFacade,
    private val config:  GuiConfig
)
```

---

### API Stability Rollout (Risk 5 fix)

```kotlin
// All api/ symbols annotated:
@RequiresOptIn(message = "LiveEconomy API is experimental. May change in v4.1.")
annotation class ExperimentalLiveEconomyAPI

@ExperimentalLiveEconomyAPI
interface MarketService { ... }
```

| Version | Status |
|---|---|
| v4.0 | `@ExperimentalLiveEconomyAPI` — may change |
| v4.1 | `@Stable` — guaranteed stable for v4.x |
| v5.0 | Breaking changes allowed with migration guide |

---

## Composition Root

```kotlin
override fun onEnable() {
    // 1. Platform
    val scheduler = SchedulerImpl(this)
    val configs   = ConfigLoader.load(dataFolder)

    // 2. Item mapping — ALL Material conversions go through here
    val nexo      = NexoIntegration(server)
    val mapper    = BukkitItemKeyMapper(nexo)

    // 3. Integrations
    val vault     = VaultGateway(server)   // implements EconomyGateway

    // 4. Storage
    val storage   = StorageFactory.create(configs.storage)

    // 5. Core services (all params are interfaces)
    val pricing   : PricingEngine    = PriceModelImpl(storage.price(), modifiers = listOf())
    val pricesSvc : PriceService     = PriceServiceImpl(storage.price(), pricing, mapper)
    val tradeSvc  : TradeService     = TradeServiceImpl(storage.price(), pricing)
    val querySvc  : MarketQueryService = MarketQueryServiceImpl(storage.price(), mapper)
    val walletSvc : WalletService    = WalletServiceImpl(storage.wallet(), vault)
    val portfolioSvc: PortfolioService = PortfolioServiceImpl(storage.portfolio(), mapper)

    // 6. Domain event bus
    val domainBus : DomainEventBus   = DomainEventBusImpl()
    domainBus.start()
    domainBus.subscribe(AlertChecker(pricesSvc))
    domainBus.subscribe(AnalyticsService(storage.transaction()))
    domainBus.subscribe(BukkitEventBridge(this))

    // 7. Use cases
    val tradingUC = ExecuteTradeUseCase(tradeSvc, walletSvc, portfolioSvc, domainBus)
    val openShortUC = OpenShortUseCase(tradeSvc, walletSvc, portfolioSvc, domainBus)

    // 8. Shock registry
    val shocks = ShockRegistry(listOf(
        MiningShock(pricesSvc, domainBus, configs.events),
        HarvestShock(pricesSvc, domainBus, configs.events),
        // ...
    ))

    // 9. Facade for GUI + commands
    val facade = EconomyFacade(pricesSvc, tradeSvc, walletSvc, portfolioSvc, querySvc)

    // 10. GUI factory
    val guiFactory = GuiFactory(facade, mapper, configs.gui)

    // 11. Commands
    getCommand("market")?.setExecutor(MarketCommand(facade, guiFactory, querySvc))
    getCommand("leconomy")?.setExecutor(AdminCommand(pricesSvc, walletSvc, scheduler))
    getCommand("wallet")?.setExecutor(WalletCommand(walletSvc, guiFactory))
    getCommand("portfolio")?.setExecutor(PortfolioCommand(portfolioSvc, guiFactory))
    getCommand("short")?.setExecutor(ShortCommand(openShortUC, guiFactory))

    // 12. Listeners (inject → register)
    val playerListener = PlayerListener(walletSvc, portfolioSvc)
    val shockListener  = ShockListener(shocks, mapper)
    server.pluginManager.registerEvents(playerListener, this)
    server.pluginManager.registerEvents(shockListener, this)

    // 13. Tasks (Lifecycle)
    val tradeQueue = AsyncTradeQueue(tradingUC, scheduler)
    val tickTask   = MarketTickTask(pricesSvc, scheduler, configs.market)
    val saveTask   = AutoSaveTask(storage, scheduler)
    tradeQueue.start(); tickTask.start(); saveTask.start()

    // 14. ServiceLocator — LAST, only for Bukkit-forced contexts
    ServiceLocator.init(pricesSvc, walletSvc, portfolioSvc)

    // 15. Public API
    LiveEconomyAPI.init(pricesSvc, tradeSvc, walletSvc, portfolioSvc)
}

override fun onDisable() {
    // Reverse order
    saveTask.stop()
    tickTask.stop()
    tradeQueue.stop()
    domainBus.stop()
    storage.close()
}
```

---

## Migration Phases

### Phase 1 — Foundation (zero blast radius)
New files only. Old code untouched. Plugin still runs.

Deliverables:
- `util/` → 4 individual files (`ChatUtil`, `SoundUtil`, `InventoryUtil`, `MoneyFormat`)
- `data/model/` → 8 individual model files (split from `Models.kt`)
- `data/config/` → 6 typed config classes
- `api/item/ItemKey.kt` + `api/item/ItemKeyMapper.kt`
- `core/item/VanillaItemKey.kt` + `core/item/NexoItemKey.kt` + `core/item/BukkitItemKeyMapper.kt`
- `platform/Lifecycle.kt`

**Exit criteria:** All new files compile. Zero changes to existing files. Plugin boots identically.

### Phase 2 — API Layer (zero blast radius)
New interfaces only. No implementations.

Deliverables:
- All `api/` interface files
- Scheduler, DomainEventBus, store interfaces
- Extension point interfaces (PriceModifier, ShockHandler, TradeHook)
- `@ExperimentalLiveEconomyAPI` annotation
- `LiveEconomyAPI.kt` stub

**Exit criteria:** All `api/` files compile. External plugin can reference them.

### Phase 3 — Core Split (HIGH blast radius)
Work branch-by-branch within Phase 3:

**Branch 3a — Economy split:**
- `MarketEngine.kt` → `PriceServiceImpl` + `TradeServiceImpl` + `MarketQueryServiceImpl` + `PriceModelImpl` + `OrderBook`

**Branch 3b — Player split:**
- `Managers.kt` → `WalletServiceImpl`, `PortfolioServiceImpl`, `RoleService`, `PrestigeService`, `AlertService`, `AnalyticsService`, `MarginService`

**Branch 3c — Events split:**
- `DomainEventBusImpl` + `BukkitEventBridge`
- `ShockRegistry` + all 10 `*Shock` files

**Branch 3d — Use cases:**
- `ExecuteTradeUseCase`, `OpenShortUseCase`, `CloseShortUseCase`

**Checkpoint before merging 3a:** "Is PriceService / TradeService API correct?"
Changes after this point are expensive.

### Phase 4 — Storage (medium blast radius)
- `YamlStorageProvider` + 4 YAML stores
- `SqlStorageProvider` + 4 SQL stores + `SqlTransactionScope`
- `YamlTransactionScope` (no-op with warning)
- Wire through `StorageProvider` SPI

### Phase 5 — GUI Split (low blast radius)
- `Guis.kt` → 5 individual files
- `GuiFactory` with `EconomyFacade`
- Constructor injection — no `plugin.xxx`

### Phase 6 — Commands (low blast radius)
- `CommandNode` + `SubCommand` framework
- All subcommand files
- Constructor injection throughout

### Phase 7 — Integrations + Platform (low blast radius)
- Move to `integration/`, `platform/scheduler/`, `platform/listener/`, `platform/config/`
- `SchedulerImpl` implementing `Scheduler` interface

### Phase 8 — Bootstrap Cleanup (low blast radius)
- `LiveEconomy.kt` → composition root as shown above
- Remove all deprecated wrappers
- Verify zero `plugin.xxx` references outside `LiveEconomy.kt`
- Verify zero `*Manager` class names

**Final verification:**
- Zero files > 300 lines
- Zero god files
- Zero circular dependencies
- All 11 DI rules pass for every file

---

## Forbidden Patterns (Quick Reference)

```kotlin
// ❌ ServiceLocator outside platform/
val market = ServiceLocator.market

// ❌ Plugin reference outside LiveEconomy.kt
plugin.marketEngine.executeBuy(...)

// ❌ Manager naming
class WalletManager

// ❌ Circular service dependency
class WalletServiceImpl(private val market: MarketService)
class MarketServiceImpl(private val wallet: WalletService)

// ❌ Concrete dependency
class WalletServiceImpl(private val vault: VaultIntegration)

// ❌ Fat interface (>5 methods, unrelated operations)
interface MarketService { getPrice(); executeBuy(); executeSell(); getHistory(); getStats(); getIndex(); registerModifier() }

// ❌ EconomyFacade in core/
class ExecuteTradeUseCase(private val economy: EconomyFacade)

// ❌ Material used where ItemKey expected
fun getPrice(material: Material): Double  // in api/ layer

// ❌ Bukkit call in service
class WalletServiceImpl { fun deposit(...) { player.sendMessage(...) } }

// ❌ Business logic in factory
class GuiFactory { fun create() { val price = market.getPrice(...); return GUI(price) } }
```
