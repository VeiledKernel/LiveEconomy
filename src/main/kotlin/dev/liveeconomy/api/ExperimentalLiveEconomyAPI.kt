package dev.liveeconomy.api

/**
 * Marks LiveEconomy API symbols as experimental.
 *
 * The API may change between v4.x minor versions.
 * Stability is guaranteed from v4.1 onward.
 *
 * External plugins must opt in:
 * ```kotlin
 * @OptIn(ExperimentalLiveEconomyAPI::class)
 * class MyIntegration { ... }
 * ```
 *
 * @since 4.0
 */
@RequiresOptIn(
    level   = RequiresOptIn.Level.WARNING,
    message = "LiveEconomy API is experimental and may change in v4.1. " +
              "Opt in with @OptIn(ExperimentalLiveEconomyAPI::class)."
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS
)
annotation class ExperimentalLiveEconomyAPI
