package dev.liveeconomy.core.player

sealed class PrestigeResult {
    data class Success(val newLevel: Int) : PrestigeResult()
    data class NotEligible(
        val requiredPnl: Double,
        val requiredPnlFormatted: String
    ) : PrestigeResult()
    object MaxLevel : PrestigeResult()
}
