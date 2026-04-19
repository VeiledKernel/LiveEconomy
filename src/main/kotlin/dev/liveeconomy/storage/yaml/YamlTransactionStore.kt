package dev.liveeconomy.storage.yaml

import dev.liveeconomy.api.storage.TransactionStore
import dev.liveeconomy.api.item.ItemKeyMapper
import dev.liveeconomy.data.model.TradeAction
import dev.liveeconomy.data.model.Transaction
import dev.liveeconomy.storage.yaml.AtomicYamlWriter
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * YAML-backed [TransactionStore].
 *
 * File: `plugins/LiveEconomy/transactions.yml`
 * Capped at [MAX_PER_PLAYER] transactions per player — oldest pruned on write.
 */
class YamlTransactionStore(
    private val file:   File,
    private val mapper: ItemKeyMapper
) : TransactionStore {

    private val txs      = ConcurrentHashMap<UUID, ArrayDeque<Transaction>>()
    private val fileLock = Any()

    companion object {
        const val MAX_PER_PLAYER = 100
    }

    fun load() {
        if (!file.exists()) return
        val yaml    = YamlConfiguration.loadConfiguration(file)
        val players = yaml.getConfigurationSection("players") ?: return

        for (uuidStr in players.getKeys(false)) {
            val uuid  = try { UUID.fromString(uuidStr) } catch (e: Exception) { continue }
            val count = yaml.getInt("players.$uuidStr.count", 0)
            val deque = ArrayDeque<Transaction>(count)

            for (i in 0 until count) {
                val base = "players.$uuidStr.txs.$i"
                try {
                    val itemKey = mapper.fromString(yaml.getString("$base.item") ?: continue)
                    deque.addLast(Transaction(
                        playerUuid = uuid,
                        timestamp  = yaml.getLong("$base.timestamp"),
                        item       = itemKey,
                        action     = TradeAction.valueOf(yaml.getString("$base.action") ?: "BUY"),
                        quantity   = yaml.getInt("$base.quantity"),
                        unitPrice  = yaml.getDouble("$base.unitPrice"),
                        total      = yaml.getDouble("$base.total")
                    ))
                } catch (e: Exception) {
                    System.err.println("[YamlTransactionStore] Skipping malformed tx $i for $uuidStr: ${e.message}")
                }
            }
            if (deque.isNotEmpty()) txs[uuid] = deque
        }
    }

    override fun append(tx: Transaction) {
        val deque = txs.getOrPut(tx.playerUuid) { ArrayDeque() }
        synchronized(deque) {
            deque.addFirst(tx) // newest first
            while (deque.size > MAX_PER_PLAYER) deque.removeLast()
        }
        persist()
    }

    override fun getRecent(playerUuid: UUID, limit: Int): List<Transaction> =
        txs[playerUuid]?.take(limit) ?: emptyList()

    override fun getAll(playerUuid: UUID): List<Transaction> =
        txs[playerUuid]?.toList() ?: emptyList()

    private fun persist() {
        synchronized(fileLock) {
            val yaml = YamlConfiguration()
            for ((uuid, deque) in txs) {
                val list = synchronized(deque) { deque.toList() }
                yaml.set("players.$uuid.count", list.size)
                list.forEachIndexed { i, tx ->
                    val base = "players.$uuid.txs.$i"
                    yaml.set("$base.timestamp", tx.timestamp)
                    yaml.set("$base.item",      tx.item.id)
                    yaml.set("$base.action",    tx.action.name)
                    yaml.set("$base.quantity",  tx.quantity)
                    yaml.set("$base.unitPrice", tx.unitPrice)
                    yaml.set("$base.total",     tx.total)
                }
            }
            file.parentFile?.mkdirs()
            AtomicYamlWriter.save(yaml, file)
        }
    }
}
