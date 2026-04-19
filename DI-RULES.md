# LiveEconomy v4.0 — Dependency Injection Rules (v3, LOCKED)

---

## Core Principle

Every class declares ONLY what it truly needs, via its constructor, as interfaces.
No shortcuts. No optional magic. No runtime service lookup except in the three
explicitly permitted platform contexts.

---

## Rule 1 — All Dependencies Must Be Interfaces

```kotlin
// ✅ Correct
class WalletServiceImpl(
    private val store:   WalletStore,     // interface
    private val economy: EconomyGateway   // interface
) : WalletService

// ❌ Forbidden
class WalletServiceImpl(
    private val repo:  WalletRepository,  // concrete
    private val vault: VaultIntegration   // concrete
)
```

**Exceptions** (must be documented):
1. Purely internal class with no alternative implementation and never mocked
2. Stateless, final, utility-like class (`MoneyFormat`, `ItemBuilder`)

Every exception: `// No interface: stateless utility, never swapped`

---

## Rule 2 — Constructor Parameters Are Non-Null and Required

```kotlin
// ✅ Correct
class TradingUseCase(
    private val market:    MarketService,
    private val wallet:    WalletService,
    private val portfolio: PortfolioService
)

// ❌ Forbidden — nullable
class TradingUseCase(private val market: MarketService?)

// ❌ Forbidden — default value hiding a required dependency
class TradingUseCase(private val market: MarketService = MarketServiceImpl())
```

---

## Rule 3 — No `new` Outside Composition Root and Factories

**Allowed in:** `LiveEconomy.onEnable()` and `*Factory` classes only.

```kotlin
// ❌ Forbidden — constructing a collaborator inside a service
class MarketServiceImpl(...) {
    private val cache = PriceCacheImpl()   // construct via injection
}

// ✅ Allowed — internal value objects and data structures
class MarketServiceImpl(...) {
    private val priceCache = mutableMapOf<ItemKey, Double>()  // local state only
}
```

---

## Rule 4 — Factories Are Pure Assemblers

```kotlin
// ✅ Correct
class GuiFactory(
    private val market:    MarketService,
    private val wallet:    WalletService,
    private val portfolio: PortfolioService,
    private val config:    GuiConfig
) {
    fun createMarketGUI(): MarketGUI = MarketGUI(market, wallet, config)
    fun createWalletGUI(player: Player): WalletGUI = WalletGUI(wallet, portfolio, player)
}

// ❌ Forbidden — ServiceLocator in factory
class GuiFactory {
    fun create(): MarketGUI = MarketGUI(ServiceLocator.market)

// ❌ Forbidden — business logic in factory
class GuiFactory(private val market: MarketService) {
    fun create(): MarketGUI {
        val price = market.getPrice(ItemKey.diamond())  // logic — forbidden
        return MarketGUI(market, price)
    }
}
```

**Factory rules:**
- May ONLY use injected dependencies
- Must NOT access `ServiceLocator`
- Must NOT contain business logic
- Must ONLY assemble and return objects

---

## Rule 5 — ServiceLocator in Exactly Three Contexts

**Permitted context 1 — PlaceholderAPI / static reflection callbacks:**
```kotlin
// ServiceLocator used: PlaceholderAPI instantiates via reflection, no constructor control.
class LiveEconomyExpansion : PlaceholderExpansion() {
    override fun onRequest(player: Player, id: String): String =
        ServiceLocator.wallet.getBalance(player).toString()
}
```

**Permitted context 2 — Third-party APIs that instantiate your class:**
```kotlin
// ServiceLocator used: ExternalPlugin registry owns instantiation.
class LiveEconomyHook : ExternalPluginHook() {
    fun onQuery(): Double = ServiceLocator.market.getPrice(ItemKey.diamond())
}
```

**Permitted context 3 — Legacy compatibility bridges (time-limited):**
```kotlin
// ServiceLocator used: legacy bridge for pre-v4 API consumers.
// TODO: remove at v4.1 when all callers migrate to LiveEconomyAPI.get()
class LegacyBridge {
    fun getBalance(uuid: UUID): Double = ServiceLocator.wallet.getBalance(...)
}
```

**Mandatory comment on every usage:**
```kotlin
// ServiceLocator used: [specific reason]. [Which permitted context].
val wallet = ServiceLocator.wallet
```

**Forbidden in:** `core/`, `gui/`, `command/`, `storage/`, `data/`, `api/`, `util/`

---

## Rule 6 — Long-Running Components Implement Lifecycle

```kotlin
interface Lifecycle {
    fun start()
    fun stop()
}
```

**Required for:** all `*Task` classes, `AsyncTradeQueue`, `DomainEventBusImpl`,
`StorageProvider` implementations, any integration that opens a connection.

**Shutdown order:** reverse of startup — tasks first, storage last.

```kotlin
override fun onDisable() {
    tradeQueue.stop()
    saveTask.stop()
    tickTask.stop()
    storage.close()   // last
}
```

---

## Rule 7 — Services Are Thread-Agnostic

No Bukkit API calls inside `core/`. All threading concerns live in `platform/`.

```kotlin
// ❌ Forbidden — Bukkit call inside service
class WalletServiceImpl(...) {
    override fun deposit(player: Player, amount: Double) {
        player.sendMessage("Received $$amount")  // forbidden
    }
}

// ✅ Correct — service returns result; platform layer sends message
class WalletServiceImpl(...) {
    override fun deposit(player: Player, amount: Double): DepositResult { ... }
}
// Listener/task sends the message after receiving DepositResult
```

**Threading rules:**
- Services are safe to call from any thread
- Bukkit API calls live in `platform/listener/`, `platform/scheduler/`, or `gui/`
- Async→main transitions use `Scheduler.runOnMain()`
- `@Volatile` on shared mutable fields where async reads occur

---

## Rule 8 — Use Cases for Multi-Service Operations

Operations coordinating 3+ services are extracted to `core/usecase/`.

```kotlin
// ✅ Use Case — orchestrates without adding logic to any service
class ExecuteTradeUseCase(
    private val market:    MarketService,
    private val wallet:    WalletService,
    private val portfolio: PortfolioService,
    private val eventBus:  DomainEventBus
) {
    fun executeBuy(player: Player, item: ItemKey, qty: Int): TradeResult {
        val price = market.getAskPrice(item) * qty
        val withdraw = wallet.withdraw(player, price)
        if (withdraw is WithdrawResult.Insufficient) return TradeResult.InsufficientFunds
        portfolio.addHolding(player.uniqueId, item, qty)
        eventBus.publish(TradeExecutedEvent(player.uniqueId, item, qty, price))
        return TradeResult.Success(price)
    }
}
```

**When to extract:** operation touches 3+ services, has multiple failure paths,
or appears in both a command and a GUI.

---

## Rule 9 — No Circular Dependencies (Even Via Interfaces)

Services must not depend on each other in a cycle, even indirectly through interfaces.

```kotlin
// ❌ Forbidden — circular via interfaces
class MarketServiceImpl(private val wallet: WalletService)  // market needs wallet
class WalletServiceImpl(private val market: MarketService)  // wallet needs market ← cycle

// ✅ Correct — break the cycle with a Use Case
class ExecuteTradeUseCase(
    private val market: MarketService,   // neither service knows about the other
    private val wallet: WalletService
)
```

**Detection rule:** draw the dependency graph before writing any service.
If any arrow forms a loop, extract a Use Case or introduce an event.

**Common cycle patterns to watch for:**
- `MarketService ↔ WalletService` — break with `ExecuteTradeUseCase`
- `AlertService ↔ MarketService` — break with `DomainEventBus` (market publishes, alert subscribes)
- `AnalyticsService ↔ PortfolioService` — break with `TradeExecutedEvent`

---

## Rule 10 — Interface Granularity (No Fat Interfaces)

Interfaces that grow beyond ~5 methods become fat and force implementors to
implement irrelevant operations. Split early by responsibility.

```kotlin
// ❌ Fat interface
interface MarketService {
    fun getPrice(item: ItemKey): Double
    fun executeBuy(player: Player, item: ItemKey, qty: Int): TradeResult
    fun executeSell(player: Player, item: ItemKey, qty: Int): TradeResult
    fun getHistory(item: ItemKey, page: Int): List<PriceCandle>
    fun getStats(item: ItemKey): ItemStats
    fun getIndex(): Double
    fun registerModifier(modifier: PriceModifier)
}

// ✅ Split by responsibility
interface PriceService {        // read-only price queries
    fun getPrice(item: ItemKey): Double
    fun getBid(item: ItemKey): Double
    fun getAsk(item: ItemKey): Double
    fun getIndex(): Double
}

interface TradeService {        // write operations — trade execution only
    fun executeBuy(player: Player, item: ItemKey, qty: Int): TradeResult
    fun executeSell(player: Player, item: ItemKey, qty: Int): TradeResult
}

interface MarketQueryService {  // market data and history
    fun getHistory(item: ItemKey, page: Int): List<PriceCandle>
    fun getStats(item: ItemKey): ItemStats
}
```

**Granularity rule:** if a GUI only needs price data, inject `PriceService` —
not the full `MarketService`. Inject only what the class truly needs.

---

## Rule 11 — EconomyFacade for GUI and Command Injection

When a GUI or command needs more than two services, inject an `EconomyFacade`
instead of individual services. This prevents constructor explosion.

```kotlin
// ✅ Facade for presentation layer only
data class EconomyFacade(
    val price:     PriceService,
    val trade:     TradeService,
    val wallet:    WalletService,
    val portfolio: PortfolioService
)

// GUI receives facade, not 4+ services
class MarketGUI(
    private val economy: EconomyFacade,
    private val config:  GuiConfig
)

// ❌ Facade in core — forbidden
class ExecuteTradeUseCase(
    private val economy: EconomyFacade  // facade in core breaks isolation
)
```

**Facade rules:**
- Used in `gui/` and `command/` only — never in `core/`
- Is a pure data class — no methods, no logic
- Constructed in composition root, injected into factories

---

## Full Enforcement Checklist

Applied to every generated file:

**Interfaces:**
- [ ] All constructor dependencies are interfaces (exceptions documented)
- [ ] No fat interfaces — split if >5 methods
- [ ] No circular dependency cycles (graph drawn before writing)

**Constructors:**
- [ ] No nullable constructor parameters
- [ ] No constructor parameters with default values
- [ ] No internal construction of collaborators

**ServiceLocator:**
- [ ] No `ServiceLocator.*` outside the three permitted contexts
- [ ] Every usage has the mandatory comment

**Factories:**
- [ ] No ServiceLocator in factory methods
- [ ] No business logic in factory methods
- [ ] Factory uses only its injected dependencies

**Lifecycle:**
- [ ] Every task/connection implements `Lifecycle`
- [ ] `onDisable()` stops all Lifecycle components in reverse order

**Threading:**
- [ ] No Bukkit API calls in `core/`
- [ ] Async→main transitions use `Scheduler.runOnMain()`

**Use Cases:**
- [ ] 3+ service operations extracted to `core/usecase/`
- [ ] No circular dependencies (verified before writing)

**EconomyFacade:**
- [ ] Facade used only in `gui/` and `command/` layers
- [ ] Facade is a pure data class — no methods

**Naming:**
- [ ] No `*Manager`, no `*Repository`
- [ ] Service impls: `*ServiceImpl`
- [ ] Store impls: `Yaml*Store`, `Sql*Store`

**ItemKey (Rule 12):**
- [ ] `VanillaItemKey` and `NexoItemKey` are `data class`
- [ ] Equality based on `id` only — no other fields

**DomainEventBus (Rule 13):**
- [ ] Handlers do not block — heavy work delegated to `Scheduler`
- [ ] Bus is synchronous — no async dispatch inside `publish()`

**Scheduler (Rule 14):**
- [ ] No service class receives or calls `Scheduler` directly
- [ ] Only Use Cases and platform layer may schedule work

**EconomyFacade (Rule 15):**
- [ ] Facade has ≤ 5 service fields
- [ ] No facade in `core/` — gui/ and command/ only

**Use Cases (Rule 16):**
- [ ] No 3+ service orchestration in commands or GUIs
- [ ] All multi-service flows go through a Use Case

---

## Rule 12 — ItemKey Equality Contract

`ItemKey` equality MUST be based solely on `id`. Implementations must be
`data class` to get correct `equals`/`hashCode` for use in maps and caches.

```kotlin
// ✅ Correct — data class, equality on id
data class VanillaItemKey(
    override val namespace: String,
    override val key: String
) : ItemKey {
    override val id: String get() = "$namespace:$key"
}

// Rule: two ItemKeys with the same id are equal.
// No other fields may participate in equality.
```

This is mandatory. Without it: price caches silently miss, maps produce
duplicate keys, storage misidentifies items.

---

## Rule 13 — DomainEventBus Is Synchronous

`DomainEventBus.publish()` dispatches synchronously on the calling thread.

```kotlin
// Required behavior:
domainBus.publish(TradeExecutedEvent(...))
// All handlers run and complete before publish() returns.
// The next line executes only after all handlers are done.
val result = TradeResult.Success(price)
```

Heavy work inside a handler is forbidden. If a handler needs async work,
it receives a `Scheduler` and delegates:

```kotlin
// ✅ Correct — handler delegates heavy work to scheduler
class AnalyticsService(
    private val store:     TransactionStore,
    private val scheduler: Scheduler
) : DomainEventHandler {
    override fun handle(event: DomainEvent) {
        if (event !is TradeExecutedEvent) return
        scheduler.runAsync { store.recordTrade(event) }  // heavy work off the bus
    }
}

// ❌ Forbidden — blocking work on the event bus
class AnalyticsService(...) : DomainEventHandler {
    override fun handle(event: DomainEvent) {
        Thread.sleep(100)              // blocks the bus
        store.runExpensiveQuery()      // blocks the bus
    }
}
```

**Why synchronous:** deterministic ordering, testable without concurrency,
no race conditions between handlers.

---

## Rule 14 — Services Must Not Call Scheduler Directly

`Scheduler` is a platform concern. Services are thread-agnostic and must
not schedule work themselves.

```kotlin
// ❌ Forbidden — service calling scheduler
class WalletServiceImpl(
    private val store:     WalletStore,
    private val scheduler: Scheduler   // service should not receive this
) : WalletService {
    override fun deposit(player: Player, amount: Double) {
        scheduler.runOnMain { player.sendMessage("...") }  // forbidden in service
    }
}

// ✅ Correct — service returns result; caller (listener/task) handles scheduling
class WalletServiceImpl(
    private val store: WalletStore,
    private val vault: EconomyGateway
) : WalletService {
    override fun deposit(player: Player, amount: Double): DepositResult { ... }
}

// In platform/listener/PlayerListener.kt:
val result = walletService.deposit(player, amount)
if (result is DepositResult.Success) {
    scheduler.runOnMain { player.sendMessage("Received \$$amount") }
}
```

**Exception:** `ExecuteTradeUseCase` and other Use Cases may receive `Scheduler`
if they coordinate async operations across services — but only Use Cases,
never plain services.

---

## Rule 15 — EconomyFacade Hard Limit: 5 Services Max

`EconomyFacade` may contain at most 5 service references. If a GUI or command
needs more than 5 services, it is doing too much — split the GUI or command,
or introduce a second purpose-built facade.

```kotlin
// ✅ Correct — 5 services, clear purpose
data class EconomyFacade(
    val price:     PriceService,
    val trade:     TradeService,
    val wallet:    WalletService,
    val portfolio: PortfolioService,
    val query:     MarketQueryService
)

// ❌ Forbidden — facade creep
data class EconomyFacade(
    val price:     PriceService,
    val trade:     TradeService,
    val wallet:    WalletService,
    val portfolio: PortfolioService,
    val query:     MarketQueryService,
    val alert:     AlertService,       // 6th — split the GUI instead
    val analytics: AnalyticsService,   // 7th — this is a god object
    val prestige:  PrestigeService     // 8th — stop
)
```

If a split is needed:
```kotlin
data class TradingFacade(val price: PriceService, val trade: TradeService, val wallet: WalletService)
data class ProfileFacade(val wallet: WalletService, val portfolio: PortfolioService, val prestige: PrestigeService)
```

---

## Rule 16 — Use Cases Are Mandatory for 3+ Service Operations

Any operation that touches 3 or more services MUST go through a Use Case.
Direct service orchestration across 3+ services in a command or GUI is forbidden.

```kotlin
// ❌ Forbidden — orchestrating 3 services directly in a command
class ShortCommand(private val economy: EconomyFacade) : CommandExecutor {
    override fun onCommand(...): Boolean {
        val price = economy.trade.getShortPrice(item)
        economy.wallet.lockCollateral(player, price)    // service 2
        economy.portfolio.openShort(player, item, qty)  // service 3
        // This belongs in OpenShortUseCase
    }
}

// ✅ Correct — Use Case owns the orchestration
class ShortCommand(
    private val openShort: OpenShortUseCase,  // single dependency
    private val gui:       GuiFactory
) : CommandExecutor {
    override fun onCommand(...): Boolean {
        val result = openShort.execute(player, item, qty)
        // handle result
    }
}
```
