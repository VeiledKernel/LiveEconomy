package dev.liveeconomy.integration.vault

import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Vault [Economy] implementation of [EconomyGateway].
 *
 * **Boot safety:** loaded via Bukkit's ServiceManager at construction time.
 * If Vault is absent or no economy provider is registered, [isAvailable] is
 * false and all methods are no-ops or return safe defaults. The plugin boots
 * normally in all three scenarios:
 *  - Vault not installed → [economy] is null
 *  - Vault installed, no economy provider → [economy] is null
 *  - Vault installed, economy provider present → fully operational
 *
 * **Isolation:** never imported by core/. All core wallet logic goes through
 * [EconomyGateway] — this class is the only file that references Vault APIs.
 */
class VaultGateway(server: Server) : EconomyGateway {

    private val economy: Economy? = runCatching {
        server.servicesManager.getRegistration(Economy::class.java)?.provider
    }.getOrNull()

    override val isAvailable: Boolean get() = economy != null

    // ── Online player overloads (called on main thread) ───────────────────────

    override fun getBalance(player: Player): Double? = economy?.getBalance(player)

    override fun deposit(player: Player, amount: Double) {
        economy?.depositPlayer(player, amount)
    }

    override fun withdraw(player: Player, amount: Double) {
        economy?.withdrawPlayer(player, amount)
    }

    override fun setBalance(player: Player, amount: Double) {
        economy?.let { eco ->
            val current = eco.getBalance(player)
            when {
                amount > current -> eco.depositPlayer(player, amount - current)
                amount < current -> eco.withdrawPlayer(player, current - amount)
                // equal → no-op
            }
        }
    }

    // ── UUID overloads (offline-safe) ─────────────────────────────────────────

    override fun has(uuid: UUID, amount: Double): Boolean =
        economy?.has(Bukkit.getOfflinePlayer(uuid), amount) ?: false

    override fun getBalance(uuid: UUID): Double =
        economy?.getBalance(Bukkit.getOfflinePlayer(uuid)) ?: 0.0

    override fun deposit(uuid: UUID, amount: Double) {
        economy?.depositPlayer(Bukkit.getOfflinePlayer(uuid), amount)
    }

    override fun withdraw(uuid: UUID, amount: Double) {
        economy?.withdrawPlayer(Bukkit.getOfflinePlayer(uuid), amount)
    }
}
