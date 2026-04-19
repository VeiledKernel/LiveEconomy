package dev.liveeconomy.api.player

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI
import dev.liveeconomy.api.player.result.DepositResult
import dev.liveeconomy.api.player.result.WithdrawResult
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Player balance operations.
 *
 * Bridges the internal wallet system and Vault (when enabled).
 * All methods are thread-agnostic unless otherwise stated.
 *
 * Write operations return typed results — callers must handle all cases:
 * ```kotlin
 * when (val r = wallet.withdraw(player, cost)) {
 *     is WithdrawResult.Success           -> proceed(r.newBalance)
 *     is WithdrawResult.InsufficientFunds -> tell(player, "Need ${r.available}")
 *     is WithdrawResult.InvalidAmount     -> log.warn("Bug: zero/negative amount")
 *     is WithdrawResult.ProviderFailure   -> log.warn("Vault error: ${r.reason}")
 * }
 * ```
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
interface WalletService {

    /** Current balance for [player]. Uses Vault if enabled, internal wallet otherwise. */
    fun getBalance(player: Player): Double

    /** Current balance by UUID — offline-player safe. */
    fun getBalance(uuid: UUID): Double

    /** Whether [player] has at least [amount] in their balance. */
    fun has(player: Player, amount: Double): Boolean

    /**
     * Deposit [amount] into [player]'s balance.
     * Returns [DepositResult.Success] or a typed failure.
     */
    fun deposit(player: Player, amount: Double): DepositResult

    /**
     * Withdraw [amount] from [player]'s balance.
     * Returns [WithdrawResult.Success] or a typed failure.
     * [WithdrawResult.InsufficientFunds.available] holds the actual balance
     * so callers can show a precise error without a second [getBalance] call.
     */
    fun withdraw(player: Player, amount: Double): WithdrawResult

    /**
     * Set [player]'s balance to exactly [amount].
     * Admin operation — must only be called from permission-gated admin commands.
     */
    fun setBalance(player: Player, amount: Double)
}
