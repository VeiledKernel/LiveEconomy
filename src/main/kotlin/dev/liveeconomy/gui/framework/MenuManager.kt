package dev.liveeconomy.gui.framework

import dev.liveeconomy.api.scheduler.Scheduler
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.Stack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

typealias SlotHandler = (player: Player, isRightClick: Boolean) -> Unit

/**
 * A managed GUI inventory with self-contained slot click handlers.
 *
 * Slots are registered with handlers — no raw InventoryClickEvent routing
 * in individual GUIs. [MenuManager] dispatches clicks automatically.
 */
class LiveMenu(
    val title:   String,
    val rows:    Int,
    private val onClose: ((Player) -> Unit)? = null
) {
    private val titleComponent = LEGACY.deserialize(title)
    private val handlers       = HashMap<Int, SlotHandler>()
    private val staticItems    = HashMap<Int, ItemStack>()

    fun setItem(slot: Int, item: ItemStack) {
        staticItems[slot] = item
        handlers.remove(slot)
    }

    fun setSlot(slot: Int, item: ItemStack, onClick: SlotHandler) {
        staticItems[slot] = item
        handlers[slot]    = onClick
    }

    fun setSlot(slot: Int, item: ItemStack, onClick: (player: Player) -> Unit) {
        staticItems[slot] = item
        handlers[slot]    = { player, _ -> onClick(player) }
    }

    fun build(): Inventory =
        Bukkit.createInventory(null, rows * 9, titleComponent)
            .also { inv -> staticItems.forEach { (slot, item) -> inv.setItem(slot, item) } }

    fun handleClick(player: Player, slot: Int, isRightClick: Boolean) =
        handlers[slot]?.invoke(player, isRightClick)

    fun handleClose(player: Player) = onClose?.invoke(player)

    fun isTitleMatch(component: Component): Boolean =
        LEGACY.serialize(component).startsWith(LEGACY.serialize(titleComponent))

    companion object {
        val LEGACY: LegacyComponentSerializer = LegacyComponentSerializer.legacySection()
    }
}

/** Per-player navigation state — stack-based menu history. */
data class MenuSession(
    var currentMenu: LiveMenu,
    val history: Stack<LiveMenu> = Stack()
)

/**
 * Central dispatcher for all LiveEconomy GUI events.
 *
 * No longer an `object` — receives [Scheduler] via constructor (DI Rule 1).
 * Register as a Bukkit listener in the composition root.
 *
 * No [plugin.xxx] references — all scheduling through [Scheduler].
 */
class MenuManager(private val scheduler: Scheduler) : Listener {

    private val sessions = ConcurrentHashMap<UUID, MenuSession>()

    // ── Open / Navigation ─────────────────────────────────────────────────────

    fun open(player: Player, menu: LiveMenu, pushHistory: Boolean = false) {
        val session = sessions[player.uniqueId]
        if (pushHistory && session != null) {
            session.history.push(session.currentMenu)
            session.currentMenu = menu
        } else {
            sessions[player.uniqueId] = MenuSession(menu)
        }
        // Open on next tick to prevent cursor desync
        scheduler.runOnMain {
            player.setItemOnCursor(null)
            player.openInventory(menu.build())
        }
    }

    fun openAndStack(player: Player, menu: LiveMenu) = open(player, menu, pushHistory = true)

    fun kickBack(player: Player) {
        val session = sessions[player.uniqueId] ?: run {
            player.closeInventory(); return
        }
        if (session.history.isEmpty()) {
            sessions.remove(player.uniqueId)
            scheduler.runOnMain { player.setItemOnCursor(null); player.closeInventory() }
        } else {
            val previous = session.history.pop()
            session.currentMenu = previous
            scheduler.runOnMain { player.setItemOnCursor(null); player.openInventory(previous.build()) }
        }
    }

    fun clearSession(player: Player) = sessions.remove(player.uniqueId)
    fun getSession(player: Player): MenuSession? = sessions[player.uniqueId]
    fun isInMenu(player: Player): Boolean = sessions.containsKey(player.uniqueId)

    // ── Bukkit event handlers ─────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player  = event.whoClicked as? Player ?: return
        val session = sessions[player.uniqueId] ?: return
        if (!session.currentMenu.isTitleMatch(event.view.title())) return
        event.isCancelled = true
        val slot = event.rawSlot
        if (slot < 0 || slot >= event.inventory.size) return
        session.currentMenu.handleClick(player, slot, event.isRightClick)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        if (sessions.containsKey(player.uniqueId)) event.isCancelled = true
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player  = event.player as? Player ?: return
        val session = sessions[player.uniqueId] ?: return
        if (!session.currentMenu.isTitleMatch(event.view.title())) return
        if (session.history.isEmpty()) {
            session.currentMenu.handleClose(player)
            sessions.remove(player.uniqueId)
        }
    }
}
