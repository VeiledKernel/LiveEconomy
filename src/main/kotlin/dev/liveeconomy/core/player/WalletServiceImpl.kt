package dev.liveeconomy.core.player

import dev.liveeconomy.api.player.WalletService
import dev.liveeconomy.api.player.result.DepositResult
import dev.liveeconomy.api.player.result.WithdrawResult
import dev.liveeconomy.api.storage.WalletStore
import dev.liveeconomy.integration.vault.EconomyGateway
import org.bukkit.entity.Player
import java.util.UUID

/**
 * [WalletService] implementation.
 *
 * Routes balance operations through [EconomyGateway] when Vault is enabled,
 * falling back to [WalletStore] (internal wallet) otherwise.
 *
 * Thread-agnostic — no Bukkit API calls. Callers handle UI responses.
 */
class WalletServiceImpl(
    private val store:   WalletStore,
    private val economy: EconomyGateway
) : WalletService {

    override fun getBalance(player: Player): Double =
        economy.getBalance(player) ?: store.getBalance(player.uniqueId)

    override fun getBalance(uuid: UUID): Double =
        store.getBalance(uuid)

    override fun has(player: Player, amount: Double): Boolean =
        getBalance(player) >= amount

    override fun deposit(player: Player, amount: Double): DepositResult {
        if (amount <= 0.0) return DepositResult.InvalidAmount
        return try {
            economy.deposit(player, amount)
                ?: store.setBalance(player.uniqueId, store.getBalance(player.uniqueId) + amount)
            DepositResult.Success(newBalance = getBalance(player))
        } catch (e: Exception) {
            DepositResult.ProviderFailure(e.message ?: "Unknown error")
        }
    }

    override fun withdraw(player: Player, amount: Double): WithdrawResult {
        if (amount <= 0.0) return WithdrawResult.InvalidAmount
        val balance = getBalance(player)
        if (balance < amount) return WithdrawResult.InsufficientFunds(available = balance)
        return try {
            economy.withdraw(player, amount)
                ?: store.setBalance(player.uniqueId, balance - amount)
            WithdrawResult.Success(newBalance = getBalance(player))
        } catch (e: Exception) {
            WithdrawResult.ProviderFailure(e.message ?: "Unknown error")
        }
    }

    override fun setBalance(player: Player, amount: Double) {
        economy.setBalance(player, amount)
        store.setBalance(player.uniqueId, amount)
    }
}
