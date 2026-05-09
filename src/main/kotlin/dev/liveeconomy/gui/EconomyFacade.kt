package dev.liveeconomy.gui

import dev.liveeconomy.api.economy.MarketQueryService
import dev.liveeconomy.api.economy.PriceService
import dev.liveeconomy.api.economy.TradeService
import dev.liveeconomy.api.player.PortfolioService
import dev.liveeconomy.api.player.WalletService

/**
 * Presentation-layer facade — prevents constructor explosion in GUIs and commands.
 *
 * Inject this into GUIs and commands instead of 5+ individual services.
 * **Forbidden in core/** — would create a god-object shortcut past DI rules.
 * Max 5 service fields (Rule 15 — EconomyFacade hard limit).
 *
 * Constructed once in [dev.liveeconomy.gui.factory.GuiFactory] and passed
 * through to all GUI screens that need it.
 */
data class EconomyFacade(
    val price:     PriceService,
    val trade:     TradeService,
    val wallet:    WalletService,
    val portfolio: PortfolioService,
    val query:     MarketQueryService
)
