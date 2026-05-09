package dev.liveeconomy.platform.config

import dev.liveeconomy.api.economy.MarketQueryService
import dev.liveeconomy.core.market.MarketRegistry
import org.bukkit.plugin.java.JavaPlugin

/**
 * Orchestrates a runtime reload of market item definitions.
 *
 * Flow: AdminReloadCommand → reload() → clearAll → CategoryLoader.load → rebuildCache → log
 *
 * Calling [reload] twice is safe: [MarketRegistry.clearAll] runs first, so items never double.
 */
class RuntimeReloadService(
    private val plugin:         JavaPlugin,
    private val registry:       MarketRegistry,
    private val categoryLoader: CategoryLoader,
    private val queryService:   MarketQueryService
) {
    data class ReloadResult(val categoryCount: Int, val itemCount: Int, val skippedCount: Int, val elapsedMs: Long)

    fun reload(): ReloadResult {
        val start = System.currentTimeMillis()
        registry.clearAll()
        val loaded = categoryLoader.load()
        queryService.rebuildCache()
        val elapsed = System.currentTimeMillis() - start
        plugin.logger.info("[LiveEconomy] Reload complete in ${elapsed}ms — " +
            "${loaded.categoryCount} categories, ${loaded.itemCount} items" +
            if (loaded.skippedCount > 0) ", ${loaded.skippedCount} skipped" else "")
        return ReloadResult(loaded.categoryCount, loaded.itemCount, loaded.skippedCount, elapsed)
    }
}
