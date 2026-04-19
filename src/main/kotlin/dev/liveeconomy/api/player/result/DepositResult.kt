package dev.liveeconomy.api.player.result

import dev.liveeconomy.api.ExperimentalLiveEconomyAPI

/**
 * Typed result of a wallet deposit operation.
 *
 * @since 4.0 (Experimental)
 */
@ExperimentalLiveEconomyAPI
sealed interface DepositResult {

    /** Deposit succeeded. [newBalance] is the balance after the operation. */
    data class Success(val newBalance: Double) : DepositResult

    /** Amount was zero or negative. */
    data object InvalidAmount : DepositResult

    /** Economy provider (Vault) rejected the operation. */
    data class ProviderFailure(val reason: String) : DepositResult
}
