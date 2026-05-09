package dev.liveeconomy.platform.config

import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.core.market.MarketRegistry
import dev.liveeconomy.data.model.MarketCategory
import dev.liveeconomy.data.model.MarketItem
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * Loads market categories and items from `plugins/LiveEconomy/categories/*.yml`.
 *
 * **Boot order:** must run BEFORE [dev.liveeconomy.platform.scheduler.MarketTickTask] starts.
 * **Error handling:** malformed files and invalid items are skipped with warnings.
 * Duplicate item IDs within a single load are detected and skipped.
 * **Reload safety:** [MarketRegistry.clearAll] must be called before [load] to prevent duplication.
 */
class CategoryLoader(
    private val plugin:   JavaPlugin,
    private val mapper:   ItemKeyMapper,
    private val registry: MarketRegistry
) {
    data class LoadResult(val categoryCount: Int, val itemCount: Int, val skippedCount: Int)

    fun load(): LoadResult {
        val categoriesDir = File(plugin.dataFolder, "categories")
        if (!categoriesDir.exists() || categoriesDir.listFiles()?.isEmpty() == true) {
            saveDefaults(categoriesDir)
        }

        val ymlFiles = categoriesDir.listFiles { f -> f.extension == "yml" }
            ?: return LoadResult(0, 0, 0).also {
                plugin.logger.warning("[LiveEconomy] categories/ is empty — market will have no items.")
            }

        var totalItems = 0; var totalSkipped = 0
        val loadedCategories = mutableSetOf<String>()
        val loadedItemIds    = mutableSetOf<String>()

        for (file in ymlFiles.sortedBy { it.name }) {
            runCatching { loadFile(file, loadedItemIds) }.fold(
                onSuccess = { (items, skipped) ->
                    if (items > 0 || skipped > 0) loadedCategories += file.nameWithoutExtension
                    totalItems += items; totalSkipped += skipped
                },
                onFailure = { ex ->
                    plugin.logger.warning("[LiveEconomy] Failed to load ${file.name}: ${ex.message}")
                }
            )
        }

        plugin.logger.info("[LiveEconomy] Loaded ${loadedCategories.size} categories " +
            "($totalItems items${if (totalSkipped > 0) ", $totalSkipped skipped" else ""}).")
        return LoadResult(loadedCategories.size, totalItems, totalSkipped)
    }

    private fun loadFile(file: File, loadedItemIds: MutableSet<String>): Pair<Int, Int> {
        val cfg      = YamlConfiguration.loadConfiguration(file)
        val catId    = cfg.getString("category") ?: file.nameWithoutExtension
        val display  = cfg.getString("display-name") ?: catId.replaceFirstChar { it.uppercase() }
        val category = MarketCategory(id = catId, displayName = display)
        val section  = cfg.getConfigurationSection("items") ?: return Pair(0, 0)
        var loaded = 0; var skipped = 0

        for (itemId in section.getKeys(false)) {
            val sec    = section.getConfigurationSection(itemId) ?: continue
            val fullId = if (itemId.contains(":")) itemId else "minecraft:$itemId"
            if (fullId in loadedItemIds) {
                plugin.logger.warning("[LiveEconomy] Duplicate item '$fullId' in ${file.name} — skipping.")
                skipped++; continue
            }
            runCatching {
                val basePrice = sec.getDouble("base-price").takeIf { it > 0 }
                    ?: error("base-price must be > 0")
                val item = MarketItem(
                    itemKey        = mapper.fromString(fullId),
                    basePrice      = basePrice,
                    baseVolatility = sec.getDouble("volatility", 0.1).coerceIn(0.0, 1.0),
                    category       = category,
                    minPrice       = sec.getDouble("min-price", basePrice * 0.1),
                    maxPrice       = sec.getDouble("max-price", basePrice * 10.0),
                    displayName    = sec.getString("display-name") ?: itemId
                )
                registry.register(item)
                loadedItemIds += fullId
                loaded++
            }.onFailure { ex ->
                plugin.logger.warning("[LiveEconomy] Skipping '$itemId' in ${file.name}: ${ex.message}")
                skipped++
            }
        }
        return Pair(loaded, skipped)
    }

    private fun saveDefaults(categoriesDir: File) {
        categoriesDir.mkdirs()
        listOf("farming.yml", "metals.yml", "gems.yml", "mob.yml").forEach { name ->
            runCatching { plugin.saveResource("categories/$name", false) }
        }
        plugin.logger.info("[LiveEconomy] First boot — default categories created.")
    }
}
