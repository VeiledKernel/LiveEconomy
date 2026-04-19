package dev.liveeconomy.core.player

import dev.liveeconomy.data.config.RolesConfig
import dev.liveeconomy.data.model.PlayerRole
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages player trading roles and their bonuses.
 *
 * Role changes are subject to a cooldown in [RolesConfig].
 * All methods are thread-safe via [ConcurrentHashMap].
 *
 * // No interface: single implementation, no swappable alternative needed.
 */
class RoleService(private val config: RolesConfig) {

    private val roles      = ConcurrentHashMap<UUID, PlayerRole>()
    private val lastChange = ConcurrentHashMap<UUID, Long>()

    fun getRole(uuid: UUID): PlayerRole =
        roles.getOrDefault(uuid, PlayerRole.NONE)

    fun canChangeRole(uuid: UUID): Boolean {
        val last = lastChange[uuid] ?: return true
        return System.currentTimeMillis() - last >= config.roleChangeCooldownMs
    }

    fun setRole(uuid: UUID, role: PlayerRole): Boolean {
        if (!canChangeRole(uuid)) return false
        roles[uuid]      = role
        lastChange[uuid] = System.currentTimeMillis()
        return true
    }

    fun getTaxMultiplier(uuid: UUID): Double =
        if (getRole(uuid) == PlayerRole.TRADER) config.roleTaxDiscount else 1.0

    fun getSellBonus(uuid: UUID, categoryId: String): Double = when (getRole(uuid)) {
        PlayerRole.MINER   -> if (categoryId in listOf("gems", "metals")) 1.0 + config.roleMinerBonus else 1.0
        PlayerRole.FARMER  -> if (categoryId == "farm") 1.0 + config.roleFarmerBonus else 1.0
        else               -> 1.0
    }

    fun getCrafterShockMultiplier(uuid: UUID): Double =
        if (getRole(uuid) == PlayerRole.CRAFTER) config.crafterShockMultiplier else 1.0

    fun loadAll(saved: Map<UUID, PlayerRole>) { roles.clear(); roles.putAll(saved) }
    fun getAll(): Map<UUID, PlayerRole> = roles.toMap()
}
