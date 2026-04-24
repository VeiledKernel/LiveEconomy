package dev.liveeconomy.integration.vault

import net.milkbowl.vault.economy.Economy
import org.bukkit.Server
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Vault [Economy] implementation of [EconomyGateway].
 *
 * Loaded via Bukkit's ServiceManager — no compile-time dep on Vault's impl.
 * Falls back gracefully if Vault is absent.
 */
class VaultGateway(server: Server) : EconomyGateway {

    private val economy: Economy? = server.servicesManager
        .getRegistration(Economy::class.java)?.provider

    override val isAvailable: Boolean get() = economy != null

    override fun getBalance(player: Player): Double? =
        economy?.getBalance(player)

    override fun deposit(player: Player, amount: Double) {
        economy?.depositPlayer(player, amount)
    }

    override fun withdraw(player: Player, amount: Double) {
        economy?.withdrawPlayer(player, amount)
    }

    override fun setBalance(player: Player, amount: Double) {
        economy?.let {
            val current = it.getBalance(player)
            if (current < amount) it.depositPlayer(player, amount - current)
            else it.withdrawPlayer(player, current - amount)
        }
    }

    override fun has(uuid: UUID, amount: Double): Boolean =
        economy?.has(org.bukkit.Bukkit.getOfflinePlayer(uuid), amount) ?: false

    override fun getBalance(uuid: UUID): Double =
        economy?.getBalance(org.bukkit.Bukkit.getOfflinePlayer(uuid)) ?: 0.0

    override fun deposit(uuid: UUID, amount: Double) {
        economy?.depositPlayer(org.bukkit.Bukkit.getOfflinePlayer(uuid), amount)
    }

    override fun withdraw(uuid: UUID, amount: Double) {
        economy?.withdrawPlayer(org.bukkit.Bukkit.getOfflinePlayer(uuid), amount)
    }
}
