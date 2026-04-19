package dev.liveeconomy.data.config

/**
 * Typed representation of the `economy:` block in config.yml.
 *
 * Constructed once by [ConfigLoader] at startup/reload and injected into
 * any class that needs economy settings. No raw config reads at runtime.
 */
data class EconomyConfig(
    val useVault:             Boolean,
    val blockEssentialsSell:  Boolean,
    val startingBalance:      Double,
    val currencySymbol:       String,
    val currencyName:         String,
    val currencyNamePlural:   String
)
