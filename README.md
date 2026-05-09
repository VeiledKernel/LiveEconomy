⚠️ This project is proprietary. Unauthorized use, copying, or redistribution is strictly prohibited.

# LiveEconomy v4

> A premium, real-time stock market economy plugin for Paper 1.21+ servers.
> Supply-and-demand pricing, limit orders, short selling, market shocks, and more.

---

## ⚠️ License & Usage Notice

**This repository is NOT open-source. No license is granted.**

All code is protected intellectual property of the author (NexaStudios).
No license is granted to use, copy, modify, or redistribute this code.

---

## What LiveEconomy Does

LiveEconomy replaces static shop prices with a real-time dynamic market:

- **Dynamic pricing** — prices move based on supply, demand, and server activity
- **Limit orders** — buy/sell at a target price when the market reaches it
- **Short selling** — bet against an item's price with collateral-backed positions
- **Market shocks** — gameplay events (mining, fishing, boss kills) move prices
- **Categories** — organise items into farming, metals, gems, mob drops, etc.
- **Prestige** — earn higher prestige levels by accumulating P&L
- **Roles** — trader, miner, farmer, crafter roles with unique bonuses
- **Price alerts** — notify players when a price crosses their target

---

## Requirements

| Requirement | Version |
|---|---|
| Server | Paper 1.21.4+ |
| Java | 21+ |
| Storage | YAML (default) / SQLite / MySQL |

---

## Soft Dependencies (all optional)

| Plugin | Effect if present |
|---|---|
| Vault | Shared economy with other plugins |
| PlaceholderAPI | Exposes `%liveeconomy_*%` placeholders |
| Nexo | Custom item support via `nexo:<id>` namespace |
| Essentials | `/sell` command blocked to prevent economy conflicts |

The plugin boots cleanly if none of these are installed.

---

## Installation

1. Drop `LiveEconomy.jar` into `plugins/`
2. Start the server — default `categories/` files are created automatically
3. Configure `config.yml` (storage backend, currency, market parameters)
4. Restart or run `/leconomy reload`

---

## Storage Backends

Set in `config.yml` under `storage.type`:

| Backend | Config value | Notes |
|---|---|---|
| YAML | `YAML` | Default. Best-effort, no ACID guarantees |
| SQLite | `SQLITE` | Single-file, WAL mode, HikariCP pooled |
| MySQL | `MYSQL` | Production-ready, requires credentials |

---

## Category File Format

Files live in `plugins/LiveEconomy/categories/*.yml`:

```yaml
category: farming
category-name: Farming
display-name: "§a🌾 Farming"

items:
  minecraft:wheat:
    display-name: "Wheat"
    base-price: 12.5
    volatility: 0.08      # 0.0–1.0, higher = more price movement
    spread: 0.03           # bid/ask gap fraction
    min-price: 1.0         # optional floor
    max-price: 100.0       # optional ceiling
```

Custom namespaces (Nexo): use `nexo:<item_id>` as the item key.

Invalid entries are skipped with a warning — other items continue loading.

---

## Commands

| Command | Permission | Description |
|---|---|---|
| `/market` | `liveeconomy.market` | Open market GUI |
| `/market role` | `liveeconomy.market` | Select trader role |
| `/market search` | `liveeconomy.market` | Browse all items |
| `/market stats [player]` | `liveeconomy.market` | View trader stats |
| `/wallet` | `liveeconomy.wallet` | Open wallet/profile GUI |
| `/portfolio` | `liveeconomy.portfolio` | Open portfolio GUI |
| `/invest` | `liveeconomy.invest` | Open price alerts GUI |
| `/invest alert <item> <price> <above\|below>` | `liveeconomy.invest` | Create price alert |
| `/invest prestige` | `liveeconomy.vip.prestige` | Attempt prestige |
| `/short open <item> <qty>` | `liveeconomy.vip.short` | Open short position |
| `/short close <item>` | `liveeconomy.vip.short` | Close short position |
| `/short list` | `liveeconomy.vip.short` | List open shorts |
| `/leconomy reload` | `liveeconomy.admin` | Reload categories + cache |
| `/leconomy setprice <item> <price>` | `liveeconomy.admin` | Override item price |
| `/leconomy shock <item> <pct>` | `liveeconomy.admin` | Manually fire price shock |
| `/leconomy save` | `liveeconomy.admin` | Force immediate data save |
| `/leconomy bull [pct]` | `liveeconomy.admin` | Fire market-wide bull shock |
| `/leconomy crash [pct]` | `liveeconomy.admin` | Fire market-wide crash |

---

## PlaceholderAPI Placeholders

Available when PlaceholderAPI is installed:

| Placeholder | Returns |
|---|---|
| `%liveeconomy_balance%` | Raw balance as double |
| `%liveeconomy_balance_formatted%` | Formatted with currency symbol |
| `%liveeconomy_market_index%` | Market index (1 decimal) |
| `%liveeconomy_role%` | Current trader role display name |
| `%liveeconomy_portfolio_value%` | Total holdings value (compact) |
| `%liveeconomy_pnl%` | Lifetime P&L with sign |
| `%liveeconomy_top_item%` | Highest-priced item display name |

---

## Vault Integration

When Vault is present with an economy provider:
- LiveEconomy uses Vault balances as the primary wallet
- `/market` buy/sell deducts from the Vault balance
- Other plugins see the same balance

When Vault is absent:
- LiveEconomy uses its own internal wallet (stored in `wallets.yml` or SQL)
- No economy sharing with other plugins

---

## Nexo Integration

When Nexo is installed:
- Items prefixed with `nexo:<id>` resolve to Nexo custom items
- Custom item holdings are tracked in the portfolio store (not Bukkit inventory)
- If Nexo is absent, `nexo:` items are silently skipped during category load

---

## Essentials `/sell` Blocker

When Essentials is present and `economy.block-essentials-sell: true` (default):
- The vanilla `/sell`, `/essentials:sell`, and `/es:sell` commands are cancelled
- Players are directed to `/market` instead
- Prevents bypassing LiveEconomy pricing via Essentials' static sell prices

Disable with `economy.block-essentials-sell: false` in `config.yml`.

---

## Reload Behavior

`/leconomy reload`:
1. Clears the current item registry (no duplication)
2. Re-reads all `categories/*.yml` files
3. Rebuilds the market query cache
4. Logs the count of categories, items, and any skipped entries
5. Does **not** restart market ticks, shutdown storage, or affect online players

---

## Development Status

```
✅ Phase 1–10 architecture complete
✅ Storage: YAML + SQLite + MySQL (HikariCP)
✅ Integration: Vault, PlaceholderAPI, Nexo, Essentials
✅ Test layer: JUnit 5 + MockK, architecture boundary tests
✅ Category loading with default content
✅ Reload flow (no duplication)
✅ Listener wiring: ShockListener, PlayerListener
⬜ Live Paper 1.21.4 smoke test (required before public release)
⬜ README/config documentation (in progress — this file)
```

### Known Deferred Items (v5.0)
- `WalletService` / `TradeService` expose `Player` convenience overloads in the API interface.
  Removing these is a breaking API change — deferred to v5.0.

---

*LiveEconomy v4 — NexaStudios — All rights reserved.*
