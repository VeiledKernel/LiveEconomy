package dev.liveeconomy.storage.yaml

import dev.liveeconomy.api.storage.WalletStore
import dev.liveeconomy.storage.yaml.AtomicYamlWriter
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * YAML-backed [WalletStore].
 *
 * File layout: `plugins/LiveEconomy/wallets.yml`
 * ```yaml
 * balances:
 *   "550e8400-e29b-41d4-a716-446655440000": 1500.0
 *   "6ba7b810-9dad-11d1-80b4-00c04fd430c8": 250.5
 * ```
 *
 * Write strategy: individual [setBalance] writes immediately.
 * [saveAll] is called by AutoSaveTask for bulk persistence.
 * In-memory map is the read source of truth at runtime.
 */
class YamlWalletStore(private val file: File) : WalletStore {

    private val balances = ConcurrentHashMap<UUID, Double>()
    private val fileLock = Any()

    fun load() {
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)
        val section = yaml.getConfigurationSection("balances") ?: return
        for (key in section.getKeys(false)) {
            try {
                balances[UUID.fromString(key)] = section.getDouble(key)
            } catch (e: IllegalArgumentException) {
                System.err.println("[YamlWalletStore] Skipping malformed UUID '$key'")
            }
        }
    }

    override fun getBalance(uuid: UUID): Double = balances.getOrDefault(uuid, 0.0)

    override fun setBalance(uuid: UUID, balance: Double) {
        balances[uuid] = balance
        persistSingle(uuid, balance)
    }

    override fun getAllBalances(): Map<UUID, Double> = balances.toMap()

    override fun saveAll(balances: Map<UUID, Double>) {
        this.balances.putAll(balances)
        persistAll()
    }

    private fun persistSingle(uuid: UUID, balance: Double) {
        synchronized(fileLock) {
            val yaml = if (file.exists()) YamlConfiguration.loadConfiguration(file)
                       else YamlConfiguration()
            yaml.set("balances.${uuid}", balance)
            file.parentFile?.mkdirs()
            AtomicYamlWriter.save(yaml, file)
        }
    }

    private fun persistAll() {
        synchronized(fileLock) {
            val yaml = YamlConfiguration()
            balances.forEach { (uuid, bal) -> yaml.set("balances.$uuid", bal) }
            file.parentFile?.mkdirs()
            AtomicYamlWriter.save(yaml, file)
        }
    }
}
