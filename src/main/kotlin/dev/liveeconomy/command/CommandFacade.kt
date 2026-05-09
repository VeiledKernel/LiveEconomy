package dev.liveeconomy.command

import dev.liveeconomy.core.alert.AlertService
import dev.liveeconomy.core.player.PrestigeService
import dev.liveeconomy.core.player.RoleService
import dev.liveeconomy.gui.EconomyFacade
import dev.liveeconomy.gui.factory.GuiFactory
import dev.liveeconomy.platform.config.RuntimeReloadService

/**
 * Injection container for the command layer.
 *
 * Commands need two things: services (for text-only responses) and
 * [GuiFactory] (for GUI-opening commands). This facade prevents
 * constructor explosion across 6 root commands × N subcommands.
 *
 * Mirrors [EconomyFacade] — forbidden in core/, used only in command/.
 *
 * Constructed once in [dev.liveeconomy.PluginBoot], injected into
 * all [dev.liveeconomy.command.framework.CommandNode] instances.
 */
data class CommandFacade(
    val economy:  EconomyFacade,
    val gui:      GuiFactory,
    val roles:    RoleService,
    val prestige: PrestigeService,
    val alerts:   AlertService
)
