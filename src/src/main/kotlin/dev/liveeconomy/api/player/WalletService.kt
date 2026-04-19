package dev.liveeconomy.api.player

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Player balance operations.
 *
 * Bridges the internal wallet system and Vault (when enabled).
 * All methods are thread-agnostic unless otherwise stated.
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
interface WalletService {

    /**
     * Get the current balance for [player].
     * Returns the Vault balance if Vault is enabled, otherwise the internal balance.
     */
    fun getBalance(player: Player): Double

    /**
     * Get the current balance by UUID (offline-player safe).
     */
    fun getBalance(uuid: UUID): Double

    /**
     * Deposit [amount] into [player]'s balance.
     * Returns true if the deposit succeeded.
     */
    fun deposit(player: Player, amount: Double): Boolean

    /**
     * Withdraw [amount] from [player]'s balance.
     * Returns true if the player had sufficient funds and the withdrawal succeeded.
     */
    fun withdraw(player: Player, amount: Double): Boolean

    /**
     * Whether [player] has at least [amount] in their balance.
     */
    fun has(player: Player, amount: Double): Boolean

    /**
     * Set [player]'s balance to exactly [amount].
     * Admin operation — bypasses normal transaction flow.
     */
    fun setBalance(player: Player, amount: Double)
}
