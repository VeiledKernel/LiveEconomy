package dev.liveeconomy.gui.player

import dev.liveeconomy.core.player.RoleService
import dev.liveeconomy.data.model.PlayerRole
import dev.liveeconomy.gui.framework.LiveMenu
import dev.liveeconomy.gui.framework.MenuManager
import dev.liveeconomy.gui.shared.ItemBuilder.itemStack
import dev.liveeconomy.gui.shared.Skulls
import dev.liveeconomy.gui.shared.Theme
import dev.liveeconomy.util.ChatUtil
import dev.liveeconomy.util.SoundUtil
import org.bukkit.Sound
import org.bukkit.entity.Player

/** Role selection screen. */
class RoleGUI(
    private val roleService: RoleService,
    private val menuManager: MenuManager
) {
    fun open(player: Player) {
        val currentRole = roleService.getRole(player.uniqueId)
        val menu        = LiveMenu("§0§l» §a§lSelect Role", rows = 3)
        for (i in 0 until 27) menu.setItem(i, border())

        listOf(PlayerRole.MINER, PlayerRole.TRADER, PlayerRole.FARMER, PlayerRole.CRAFTER)
            .forEachIndexed { index, role ->
                val isActive = currentRole == role
                menu.setSlot(10 + index * 2, itemStack(role.iconMaterial) {
                    name(if (isActive) "§a§l${role.displayName} §7(Active)" else "§f§l${role.displayName}")
                    lore("§7${role.description}", "",
                        if (isActive) "§a● Currently active" else "§eClick to select")
                    if (isActive) glow()
                }) { p ->
                    if (role != currentRole) {
                        if (roleService.setRole(p.uniqueId, role)) {
                            p.sendMessage("${ChatUtil.prefix()}§aRole set to §f${role.displayName}§a.")
                            SoundUtil.play(p, Sound.ENTITY_PLAYER_LEVELUP)
                            open(p)
                        } else {
                            p.sendMessage("${ChatUtil.prefix()}§cRole change is on cooldown.")
                        }
                    }
                }
            }

        menu.setSlot(22, Skulls.of(Skulls.BACK) { name("${Theme.GRAY}◀ Back") }) {
            menuManager.kickBack(it)
        }
        menuManager.openAndStack(player, menu)
    }

    private fun border() = itemStack(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }
}
