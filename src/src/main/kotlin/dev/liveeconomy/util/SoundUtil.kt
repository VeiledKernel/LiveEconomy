package dev.liveeconomy.util

import org.bukkit.Sound
import org.bukkit.entity.Player

/**
 * Sound playback helpers.
 *
 * Each action has a distinct pitch multiplier so sounds feel different even
 * when using the same base sound. Volume and pitch are scaled against the
 * player's configured master values at the call site.
 *
 * // No interface: stateless utility, never swapped
 */
object SoundUtil {

    /**
     * Play a sound at the player's location.
     *
     * @param player  the recipient
     * @param sound   the Bukkit sound enum value
     * @param volume  master volume (0.0–1.0)
     * @param pitch   pitch multiplier (0.5–2.0)
     */
    fun play(player: Player, sound: Sound, volume: Float = 0.6f, pitch: Float = 1.0f) {
        player.playSound(player.location, sound, volume.coerceIn(0f, 1f), pitch.coerceIn(0.5f, 2f))
    }

    /**
     * Play a sound identified by its config-map name string.
     * Silently ignores unrecognised names — no console spam.
     */
    fun playByName(player: Player, name: String, volume: Float = 0.6f, pitch: Float = 1.0f) {
        if (name.isBlank() || name.equals("NONE", ignoreCase = true)) return
        try {
            play(player, Sound.valueOf(name.uppercase()), volume, pitch)
        } catch (_: IllegalArgumentException) {
            // Unknown sound name in config — silently ignore
        }
    }
}
