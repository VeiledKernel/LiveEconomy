⚠️ This project is proprietary. Unauthorized use, copying, or redistribution is strictly prohibited.
# LiveEconomy

> Advanced market-driven economy system for Paper servers
> Designed for scalability, extensibility, and premium server environments

---

## ⚠️ License & Usage Notice

**This repository is NOT open-source. No license is granted.**

All code in this repository is **protected intellectual property** of the author.
**No license is granted to use this code in production, plugins, or derivatives.**
Viewing for reference purposes does not constitute a license to use, copy, or adapt.

### You are NOT allowed to:

* Copy, reuse, or redistribute this code (in whole or in part)
* Upload modified versions to public or private repositories
* Use this code in your own plugins or commercial products
* Adapt, derive, or build upon any portion of this code

### You ARE allowed to:

* View the code for reference and learning purposes only

**Viewing ≠ license.** Any other use — including partial reuse — is unauthorized
and will be treated as a violation of intellectual property rights.

---

## 📦 Project Overview

LiveEconomy is a **market-based economy engine** built for modern Minecraft servers.

Core features:

* Dynamic pricing engine
* Player-driven trading system
* Order book + limit orders
* Market shocks & events
* Modular storage (YAML / SQLite / MySQL)
* Extensible API for third-party plugins

---

## 🧱 Architecture

This project follows a **layered, DI-driven architecture**:

* `api/` — Public contracts (external plugin integration)
* `core/` — Business logic & market engine
* `data/` — Pure models & config objects
* `storage/` — Pluggable persistence backends
* `gui/` — Presentation layer
* `command/` — Command system
* `platform/` — Bukkit/Paper integration

Key principles:

* Constructor-based dependency injection
* No service locator (except controlled edge cases)
* Clear separation of concerns
* Phase-based refactor strategy

---

## 🚧 Development Status

Current stage:

* ✅ Phase 1 — Foundation complete
* ✅ Phase 2 — API layer complete
* ✅ Phase 3 — Core system refactor complete
* 🚧 Phase 4 — Storage refactor (initialized — YAML complete, SQL in progress)

---

## 📌 Notes

* This repository is publicly visible for **development transparency only**
* The project may be made private or licensed commercially at any time
* API contracts are currently **experimental and subject to change**

---

## 📫 Contact

For licensing, collaboration, or commercial inquiries:

* GitHub Issues (preferred)

---

All Rights Reserved

Copyright (c) 2026 VeiledKernel

All rights reserved. No permission is granted to use, copy,
modify, or distribute this software without explicit written consent.
