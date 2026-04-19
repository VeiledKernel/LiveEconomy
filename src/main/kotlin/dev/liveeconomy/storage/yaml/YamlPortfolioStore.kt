package dev.liveeconomy.storage.yaml

import dev.liveeconomy.api.item.ItemKey
import dev.liveeconomy.api.storage.PortfolioStore
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.data.model.PlayerStats
import dev.liveeconomy.data.model.ShortPosition
import dev.liveeconomy.storage.yaml.AtomicYamlWriter
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * YAML-backed [PortfolioStore].
 *
 * File layout: `plugins/LiveEconomy/portfolios.yml`
 * ```yaml
 * players:
 *   <uuid>:
 *     pnl: 1234.56
 *     prestige: 2
 *     holdings:
 *       "minecraft:diamond": 10
 *       "nexo:ruby": 5
 *     stats:
 *       totalBuys: 42
 *       totalSells: 38
 *       wins: 25
 *       totalVolume: 98000.0
 *       totalRoi: 340.5
 *     shorts:
 *       "nexo:ruby":
 *         quantity: 5
 *         entryPrice: 800.0
 *         collateral: 6000.0
 * ```
 */
class YamlPortfolioStore(
    private val file:   File,
    private val mapper: ItemKeyMapper
) : PortfolioStore {

    private val holdings  = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Int>>()
    private val pnl       = ConcurrentHashMap<UUID, BigDecimal>()
    private val stats     = ConcurrentHashMap<UUID, PlayerStats>()
    private val shorts    = ConcurrentHashMap<UUID, ConcurrentHashMap<String, ShortPosition>>()
    private val prestige  = ConcurrentHashMap<UUID, Int>()
    private val fileLock  = Any()

    fun load() {
        if (!file.exists()) return
        val yaml    = YamlConfiguration.loadConfiguration(file)
        val players = yaml.getConfigurationSection("players") ?: return

        for (uuidStr in players.getKeys(false)) {
            val uuid = try { UUID.fromString(uuidStr) } catch (e: Exception) { continue }
            val p    = players.getConfigurationSection(uuidStr) ?: continue

            pnl[uuid]      = BigDecimal.valueOf(p.getDouble("pnl", 0.0))
            prestige[uuid] = p.getInt("prestige", 0)

            // Holdings
            val holdMap = ConcurrentHashMap<String, Int>()
            p.getConfigurationSection("holdings")?.getKeys(false)?.forEach { itemId ->
                holdMap[itemId] = p.getInt("holdings.$itemId")
            }
            if (holdMap.isNotEmpty()) holdings[uuid] = holdMap

            // Stats
            p.getConfigurationSection("stats")?.let { s ->
                stats[uuid] = PlayerStats(
                    totalBuys   = s.getInt("totalBuys"),
                    totalSells  = s.getInt("totalSells"),
                    wins        = s.getInt("wins"),
                    totalVolume = s.getDouble("totalVolume"),
                    totalRoi    = s.getDouble("totalRoi")
                )
            }

            // Short positions
            val shortMap = ConcurrentHashMap<String, ShortPosition>()
            p.getConfigurationSection("shorts")?.getKeys(false)?.forEach { itemId ->
                val s = p.getConfigurationSection("shorts.$itemId") ?: return@forEach
                val itemKey = try { mapper.fromString(itemId) } catch (e: Exception) { return@forEach }
                shortMap[itemId] = ShortPosition(
                    playerUuid = uuid,
                    item       = itemKey,
                    quantity   = s.getInt("quantity"),
                    entryPrice = s.getDouble("entryPrice"),
                    collateral = s.getDouble("collateral")
                )
            }
            if (shortMap.isNotEmpty()) shorts[uuid] = shortMap
        }
    }

    // ── Holdings ──────────────────────────────────────────────────────────────

    override fun getHoldings(uuid: UUID): Map<ItemKey, Int> =
        holdings[uuid]?.mapKeys { mapper.fromString(it.key) } ?: emptyMap()

    override fun setHolding(uuid: UUID, item: ItemKey, quantity: Int) {
        holdings.getOrPut(uuid) { ConcurrentHashMap() }[item.id] = quantity
        persist()
    }

    override fun removeHolding(uuid: UUID, item: ItemKey) {
        holdings[uuid]?.remove(item.id)
        persist()
    }

    // ── P&L ───────────────────────────────────────────────────────────────────

    override fun getPnl(uuid: UUID): BigDecimal = pnl.getOrDefault(uuid, BigDecimal.ZERO)

    override fun addPnl(uuid: UUID, delta: BigDecimal) {
        pnl[uuid] = getPnl(uuid).add(delta)
        persist()
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    override fun getStats(uuid: UUID): PlayerStats = stats.getOrDefault(uuid, PlayerStats())

    override fun saveStats(uuid: UUID, s: PlayerStats) {
        stats[uuid] = s
        persist()
    }

    // ── Shorts ────────────────────────────────────────────────────────────────

    override fun getShortPositions(uuid: UUID): Map<ItemKey, ShortPosition> =
        shorts[uuid]?.mapKeys { mapper.fromString(it.key) } ?: emptyMap()

    override fun saveShortPosition(position: ShortPosition) {
        shorts.getOrPut(position.playerUuid) { ConcurrentHashMap() }[position.item.id] = position
        persist()
    }

    override fun removeShortPosition(uuid: UUID, item: ItemKey) {
        shorts[uuid]?.remove(item.id)
        persist()
    }

    override fun getAllShortPositions(): Map<UUID, Map<ItemKey, ShortPosition>> =
        shorts.mapValues { (_, map) -> map.mapKeys { mapper.fromString(it.key) } }

    // ── Prestige ──────────────────────────────────────────────────────────────

    override fun getPrestigeLevel(uuid: UUID): Int = prestige.getOrDefault(uuid, 0)

    override fun setPrestigeLevel(uuid: UUID, level: Int) {
        prestige[uuid] = level
        persist()
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun persist() {
        synchronized(fileLock) {
            val yaml    = YamlConfiguration()
            val allUuids = (holdings.keys + pnl.keys + stats.keys + shorts.keys + prestige.keys).toSet()

            for (uuid in allUuids) {
                val base = "players.$uuid"
                yaml.set("$base.pnl",      pnl[uuid]?.toDouble() ?: 0.0)
                yaml.set("$base.prestige",  prestige[uuid] ?: 0)

                holdings[uuid]?.forEach { (itemId, qty) ->
                    yaml.set("$base.holdings.$itemId", qty)
                }

                stats[uuid]?.let { s ->
                    yaml.set("$base.stats.totalBuys",   s.totalBuys)
                    yaml.set("$base.stats.totalSells",  s.totalSells)
                    yaml.set("$base.stats.wins",        s.wins)
                    yaml.set("$base.stats.totalVolume", s.totalVolume)
                    yaml.set("$base.stats.totalRoi",    s.totalRoi)
                }

                shorts[uuid]?.forEach { (itemId, pos) ->
                    val sp = "$base.shorts.$itemId"
                    yaml.set("$sp.quantity",   pos.quantity)
                    yaml.set("$sp.entryPrice", pos.entryPrice)
                    yaml.set("$sp.collateral", pos.collateral)
                }
            }

            file.parentFile?.mkdirs()
            AtomicYamlWriter.save(yaml, file)
        }
    }
}
