# LiveEconomy v4 — Deferred Production TODOs

Updated through pre-RC cleanup pass.

---

## ✅ Resolved

- `EconomyGateway` moved to `api/economy/` ✅
- `CategoryLoader` implemented and wired ✅
- `ShockListener` + `PlayerListener` registered ✅
- `PrestigeService` uses UUID-only (no Bukkit Player) ✅
- `MiningShock` dead `BlockBreakEvent` import removed ✅
- `BossKillShock` `EntityType` replaced with neutral string ID ✅
- `BukkitItemKeyMapper` moved from `core/item/` to `platform/item/` ✅
- `RuntimeReloadService` wires reload without duplication ✅
- Architecture boundary tests enforce all layer rules ✅

---

## Required Before Public Release

### Smoke test on real Paper 1.21.4 server
Boot and verify:
- No integrations present (Vault/PAPI/Nexo/Essentials all absent)
- All integrations present
- Malformed `categories/` file present
- `/leconomy reload` while players are online
- Market tick running at shutdown

### README / config documentation
**Status:** README.md updated this pass. `config.yml` inline comments still need a review pass.

---

## Post-Release (before v4.1)

### MockBukkit listener tests
`src/test/kotlin/dev/liveeconomy/platform/`
Covers: `ShockListener`, `PlayerListener`, `BukkitInventoryGateway`, `BukkitPlayerResolver`
Blocked by: MockBukkit version pinning for Paper 1.21.4.

### MySQL integration tests
`src/test/kotlin/dev/liveeconomy/storage/`
Blocked by: requires running MySQL — not suitable for standard CI.
Resolution: Maven profile `-Pmysql-tests`.

### PortfolioServiceImpl.addPnl atomicity on SQL
Read-then-write not atomic. Safe while trade execution is main-thread-bound.
Resolution: wrap in `SqlTransactionScope` before async trade execution.

---

## Low Priority / Future Enhancement

### HikariCP connection pool tuning
`storage/sql/mysql/MysqlStorageProvider.kt` — add `keepaliveTime`, `validationTimeout`.

### Schema migration tooling
`storage/sql/SqlStorageProvider.kt` — consider Flyway for v4.1+.

### ViewMapper caching (DEBT-VM-3)
`MarketViewBuilder` rebuilds full item list per GUI open.
Resolution: price-tick invalidation cache for 100+ item servers.

---

## Deferred to v5.0 (breaking API change)

### Player overloads in WalletService / TradeService
`core/player/WalletServiceImpl.kt` and `core/economy/TradeServiceImpl.kt` both import
`org.bukkit.entity.Player` to implement convenience overloads defined in the API interfaces.
Removing them requires changing the public API — a breaking change.
Approved exceptions are documented in `ArchitectureBoundaryTest`.
Resolution: make API UUID-only in v5.0; add platform adapter for Player → UUID.
