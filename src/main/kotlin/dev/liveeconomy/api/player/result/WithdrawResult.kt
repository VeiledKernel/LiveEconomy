package dev.liveeconomy.api.player.result

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI

/**
 * Typed result of a wallet withdrawal operation.
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
sealed interface WithdrawResult {

    /** Withdrawal succeeded. [newBalance] is the balance after the operation. */
    data class Success(val newBalance: Double) : WithdrawResult

    /** Player had insufficient funds. [available] is their actual balance. */
    data class InsufficientFunds(val available: Double) : WithdrawResult

    /** Amount was zero or negative. */
    data object InvalidAmount : WithdrawResult

    /** Economy provider (Vault) rejected the operation. */
    data class ProviderFailure(val reason: String) : WithdrawResult
}
