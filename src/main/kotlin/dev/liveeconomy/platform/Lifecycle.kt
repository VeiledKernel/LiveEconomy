package dev.liveeconomy.platform

/**
 * Moved to [dev.liveeconomy.api.Lifecycle].
 *
 * This typealias exists for source compatibility only and will be
 * removed in v4.1. Update imports to `dev.liveeconomy.api.Lifecycle`.
 *
 * @deprecated Use [dev.liveeconomy.api.Lifecycle] directly.
 */
@Deprecated(
    message = "Moved to api.Lifecycle. Update your import.",
    replaceWith = ReplaceWith("Lifecycle", "dev.liveeconomy.api.Lifecycle"),
    level = DeprecationLevel.WARNING
)
typealias Lifecycle = dev.liveeconomy.api.Lifecycle
