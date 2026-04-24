package dev.liveeconomy.platform.config

import dev.liveeconomy.data.config.EconomyConfig
import dev.liveeconomy.data.config.EventsConfig
import dev.liveeconomy.data.config.GuiConfig
import dev.liveeconomy.data.config.MarketConfig
import dev.liveeconomy.data.config.PrestigeConfig
import dev.liveeconomy.data.config.RolesConfig
import dev.liveeconomy.data.config.StorageConfig
import dev.liveeconomy.data.config.StorageType
import dev.liveeconomy.data.config.VipConfig
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * Loads all plugin configuration files into typed config objects.
 *
 * Called once at startup and on /leconomy reload.
 * Returns [AllConfigs] — a container passed to the composition root.
 *
 * All raw config.get* calls are isolated here — no other class reads
 * from the YAML files directly at runtime.
 */
class ConfigLoader(private val plugin: JavaPlugin) {

    fun load(): AllConfigs {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        val c = plugin.config

        val economy = EconomyConfig(
            useVault            = c.getBoolean("economy.use-vault", true),
            blockEssentialsSell = c.getBoolean("economy.block-essentials-sell", true),
            startingBalance     = c.getDouble("economy.starting-balance", 1000.0),
            currencySymbol      = c.getString("economy.currency-symbol") ?: "$",
            currencyName        = c.getString("economy.currency-name") ?: "Dollar",
            currencyNamePlural  = c.getString("economy.currency-name-plural") ?: "Dollars"
        )

        val market = MarketConfig(
            tickIntervalTicks        = c.getLong("market.tick-interval-ticks", 20L),
            baseLiquidity            = c.getDouble("market.base-liquidity", 100.0),
            reversionStrength        = c.getDouble("market.reversion-strength", 0.02),
            idleDecay                = c.getDouble("market.idle-decay", 0.05),
            broadcastThreshold       = c.getDouble("market.broadcast-threshold", 5.0),
            tickerPermissionRequired = c.getBoolean("market.ticker-permission-required", false),
            allowShortSelling        = c.getBoolean("market.allow-short-selling", true),
            shortCollateralRatio     = c.getDouble("market.short-collateral-ratio", 1.5),
            tradeTaxPercent          = c.getDouble("market.trade-tax-percent", 0.0),
            decayIntervalMinutes     = c.getLong("market.decay-interval-minutes", 5L),
            alertMaxPerPlayer        = c.getInt("market.alert-max-per-player", 5),
            alertEnabled             = c.getBoolean("market.alerts-enabled", true),
            marginEnabled            = c.getBoolean("market.margin-enabled", true),
            marginCallLevel          = c.getDouble("market.margin-call-level", 20.0),
            marginLiquidationLevel   = c.getDouble("market.margin-liquidation-level", 10.0)
        )

        val storage = StorageConfig(
            type                    = StorageType.fromString(c.getString("storage.type") ?: "YAML"),
            sqliteFile              = c.getString("storage.sqlite.file") ?: "economy.db",
            mysqlHost               = c.getString("storage.mysql.host") ?: "localhost",
            mysqlPort               = c.getInt("storage.mysql.port", 3306),
            mysqlDatabase           = c.getString("storage.mysql.database") ?: "liveeconomy",
            mysqlUsername           = c.getString("storage.mysql.username") ?: "root",
            mysqlPassword           = c.getString("storage.mysql.password") ?: "",
            mysqlPoolSize           = c.getInt("storage.mysql.pool-size", 5),
            autosaveIntervalMinutes = c.getLong("storage.autosave-interval-minutes", 5L)
        )

        val prestige = PrestigeConfig(
            enabled             = c.getBoolean("prestige.enabled", true),
            requiredPnl         = c.getDouble("prestige.required-pnl", 10000.0),
            maxLevel            = c.getInt("prestige.max-level", 5),
            tradeBonusPercent   = c.getDouble("prestige.trade-bonus-percent", 2.0),
            taxReductionPercent = c.getDouble("prestige.tax-reduction-percent", 1.0),
            alertLimitBonus     = c.getInt("prestige.alert-limit-bonus", 2)
        )

        val roles = RolesConfig(
            enabled                = c.getBoolean("roles.enabled", true),
            roleChangeCooldownMs   = c.getLong("roles.change-cooldown-hours", 24L) * 3_600_000L,
            roleTaxDiscount        = c.getDouble("roles.trader-tax-discount", 0.50),
            roleMinerBonus         = c.getDouble("roles.miner-sell-bonus", 0.10),
            roleFarmerBonus        = c.getDouble("roles.farmer-sell-bonus", 0.15),
            crafterShockMultiplier = c.getDouble("roles.crafter-shock-multiplier", 2.0)
        )

        val vip = VipConfig(
            taxDiscountFactor = c.getDouble("vip.tax-discount-factor", 0.90),
            extraAlertSlots   = c.getInt("vip.extra-alert-slots", 5)
        )

        val gui = loadGuiConfig()
        val events = loadEventsConfig(c)

        return AllConfigs(economy, market, storage, prestige, roles, vip, gui, events)
    }

    private fun loadGuiConfig(): GuiConfig {
        val guiFile = File(plugin.dataFolder, "gui.yml")
        if (!guiFile.exists()) plugin.saveResource("gui.yml", false)
        val g = YamlConfiguration.loadConfiguration(guiFile)

        return GuiConfig(
            title              = g.getString("market.title") ?: "&8[&6Market&8] &7{category}",
            rows               = g.getInt("market.rows", 6),
            borderMaterialName = g.getString("market.border-material") ?: "BLACK_STAINED_GLASS_PANE",
            fillerMaterialName = g.getString("market.filler-material") ?: "GRAY_STAINED_GLASS_PANE",
            itemSlots          = g.getIntegerList("market.item-slots").toIntArray(),
            borderSlots        = g.getIntegerList("market.border-slots").toIntArray(),
            buttons            = GuiConfig.ButtonSlots(
                prevPage    = g.getInt("market.buttons.prev-page", 45),
                nextPage    = g.getInt("market.buttons.next-page", 53),
                search      = g.getInt("market.buttons.search", 49),
                title       = g.getInt("market.buttons.title", 4),
                alerts      = g.getInt("market.buttons.alerts", 46),
                wallet      = g.getInt("market.buttons.wallet", 47),
                portfolio   = g.getInt("market.buttons.portfolio", 48),
                orders      = g.getInt("market.buttons.orders", 50),
                index       = g.getInt("market.buttons.index", 51),
                leaderboard = g.getInt("market.buttons.leaderboard", 52)
            )
        )
    }

    private fun loadEventsConfig(c: org.bukkit.configuration.ConfigurationSection): EventsConfig {
        fun shock(path: String) = EventsConfig.ShockConfig(
            enabled          = c.getBoolean("$path.enabled", true),
            impactMultiplier = c.getDouble("$path.impact-multiplier", 1.0)
        )
        fun threshold(path: String, cat: String = "", msg: String = "") = EventsConfig.ThresholdShockConfig(
            enabled       = c.getBoolean("$path.enabled", true),
            threshold     = c.getInt("$path.threshold", 10),
            windowSeconds = c.getInt("$path.window-seconds", 60),
            shockPercent  = c.getDouble("$path.shock-percent", -5.0),
            category      = c.getString("$path.category") ?: cat,
            message       = c.getString("$path.message") ?: msg
        )
        fun category(path: String) = EventsConfig.CategoryShockConfig(
            enabled       = c.getBoolean("$path.enabled", true),
            threshold     = c.getInt("$path.threshold", 20),
            windowSeconds = c.getInt("$path.window-seconds", 60),
            shockPercent  = c.getDouble("$path.shock-percent", -8.0),
            category      = c.getString("$path.category") ?: "farm",
            message       = c.getString("$path.message") ?: ""
        )
        fun boss(path: String) = EventsConfig.BossShock(
            shockPercent = c.getDouble("$path.shock-percent", 10.0),
            category     = c.getString("$path.category") ?: "mob",
            message      = c.getString("$path.message") ?: ""
        )

        return EventsConfig(
            enabled       = c.getBoolean("events.enabled", true),
            intervalTicks = c.getLong("events.interval-ticks", 20L),
            cooldowns     = EventsConfig.Cooldowns(
                default    = c.getInt("events.cooldowns.default", 300),
                fishing    = c.getInt("events.cooldowns.fishing", 120),
                enchanting = c.getInt("events.cooldowns.enchanting", 180),
                nightCycle = c.getInt("events.cooldowns.night-cycle", 600)
            ),
            blockBreak    = shock("events.block-break"),
            crafting      = EventsConfig.ShockConfig(c.getBoolean("events.crafting.enabled", true), 1.0),
            fishing       = EventsConfig.FishingConfig(
                enabled          = c.getBoolean("events.fishing.enabled", true),
                impactMultiplier = c.getDouble("events.fishing.impact-multiplier", 1.0),
                regularShock     = c.getDouble("events.fishing.regular-shock", -3.0),
                treasureShock    = c.getDouble("events.fishing.treasure-shock", 8.0)
            ),
            enchanting    = EventsConfig.EnchantingConfig(
                enabled   = c.getBoolean("events.enchanting.enabled", true),
                minLevel  = c.getInt("events.enchanting.min-level", 20),
                lapImpact = c.getDouble("events.enchanting.lap-impact", 1.0),
                shockPct  = c.getDouble("events.enchanting.shock-percent", 5.0),
                category  = c.getString("events.enchanting.category") ?: "gems",
                message   = c.getString("events.enchanting.message") ?: ""
            ),
            mining        = threshold("events.mining", "metals"),
            harvest       = category("events.harvest"),
            bulkCrafting  = EventsConfig.BulkCraftingConfig(
                enabled       = c.getBoolean("events.bulk-crafting.enabled", true),
                threshold     = c.getInt("events.bulk-crafting.threshold", 5),
                windowSeconds = c.getInt("events.bulk-crafting.window-seconds", 30),
                shockPercent  = c.getDouble("events.bulk-crafting.shock-percent", 5.0),
                messagePrefix = c.getString("events.bulk-crafting.message-prefix") ?: ""
            ),
            raid          = EventsConfig.RaidConfig(
                enabled      = c.getBoolean("events.raid.enabled", true),
                bonusPercent = c.getDouble("events.raid.bonus-percent", 10.0)
            ),
            bossKills     = EventsConfig.BossKillsConfig(
                enabled      = c.getBoolean("events.boss-kills.enabled", true),
                dragon       = boss("events.boss-kills.dragon"),
                wither       = boss("events.boss-kills.wither"),
                elderGuardian = boss("events.boss-kills.elder-guardian"),
                breeze       = boss("events.boss-kills.breeze")
            ),
            nightCycle    = EventsConfig.NightCycleConfig(
                enabled  = c.getBoolean("events.night-cycle.enabled", true),
                nightfall = category("events.night-cycle.nightfall"),
                dawn      = category("events.night-cycle.dawn")
            ),
            deathSpree    = threshold("events.death-spree"),
            massActivity  = EventsConfig.MassActivityConfig(
                enabled      = c.getBoolean("events.mass-activity.enabled", true),
                threshold    = c.getInt("events.mass-activity.threshold", 20),
                shockPercent = c.getDouble("events.mass-activity.shock-percent", 3.0),
                category     = c.getString("events.mass-activity.category") ?: "all",
                message      = c.getString("events.mass-activity.message") ?: ""
            )
        )
    }
}

/** Container for all typed configs — passed to the composition root. */
data class AllConfigs(
    val economy:  EconomyConfig,
    val market:   MarketConfig,
    val storage:  StorageConfig,
    val prestige: PrestigeConfig,
    val roles:    RolesConfig,
    val vip:      VipConfig,
    val gui:      GuiConfig,
    val events:   EventsConfig
)
