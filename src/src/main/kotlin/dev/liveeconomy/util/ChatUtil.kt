package dev.liveeconomy.util

/**
 * Pure chat and text utilities.
 *
 * // No interface: stateless utility, never swapped
 */
object ChatUtil {

    /**
     * Translate `&` colour codes to `§` codes.
     * Handles double `&&` as a literal ampersand.
     */
    fun colorize(text: String): String =
        text.replace(Regex("&([0-9a-fk-orA-FK-OR])")) { "§${it.groupValues[1]}" }

    /**
     * Strip all `§` colour codes from a string, returning plain text.
     */
    fun strip(text: String): String =
        text.replace(Regex("§[0-9a-fk-orA-FK-OR]"), "")

    /**
     * Broadcast a message to all online players.
     * Must only be called on the main thread.
     */
    fun broadcast(server: org.bukkit.Server, message: String) {
        server.onlinePlayers.forEach { it.sendMessage(message) }
    }

    /**
     * Replace all occurrences of [placeholders] in [text].
     * Keys should include delimiters, e.g. `"%player%"` to `"Felix"`.
     */
    fun replace(text: String, vararg placeholders: Pair<String, String>): String {
        var result = text
        for ((key, value) in placeholders) result = result.replace(key, value)
        return result
    }
}
