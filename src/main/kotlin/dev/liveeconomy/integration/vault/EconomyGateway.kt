package dev.liveeconomy.integration.vault

import org.bukkit.entity.Player
import java.util.UUID

/**
 * Interface abstraction over Vault's Economy API.
 * Keeps WalletServiceImpl free of direct Vault imports.
 */
interface EconomyGateway {
    val isAvailable: Boolean
    fun getBalance(player: Player): Double?
    fun deposit(player: Player, amount: Double)
    fun withdraw(player: Player, amount: Double)
    fun setBalance(player: Player, amount: Double)
    fun has(uuid: UUID, amount: Double): Boolean
    fun getBalance(uuid: UUID): Double
    fun deposit(uuid: UUID, amount: Double)
    fun withdraw(uuid: UUID, amount: Double)
}
