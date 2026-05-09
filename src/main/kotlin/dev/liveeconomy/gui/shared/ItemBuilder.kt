package dev.liveeconomy.gui.shared

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

/**
 * Kotlin DSL for building ItemStacks cleanly.
 *
 * Usage:
 *   val item = itemStack(Material.DIAMOND) {
 *       name("§b§lDiamond")
 *       lore("§7Worth a lot", "§7Trade wisely")
 *       amount(3)
 *       glow()
 *   }
 */
fun itemStack(material: Material, amount: Int = 1, init: ItemBuilder.() -> Unit = {}): ItemStack {
    return ItemBuilder(material, amount).apply(init).build()
}

class ItemBuilder(private val material: Material, private var amount: Int = 1) {

    private var displayName: String? = null
    private val loreLines = mutableListOf<String>()
    private val flags = mutableSetOf<ItemFlag>()
    private var glow = false
    private var customModelData: Int? = null
    private val extraMeta = mutableListOf<(ItemMeta) -> Unit>()

    fun name(name: String)  { displayName = name }
    fun amount(n: Int)      { amount = n }
    fun glow()              { glow = true }
    fun model(data: Int)    { customModelData = data }
    fun hideAll()           { flags += ItemFlag.entries.toSet() }

    fun lore(vararg lines: String) { loreLines += lines }
    fun lore(lines: List<String>)  { loreLines += lines }
    fun lore(line: String)         { loreLines += line }

    fun flag(vararg f: ItemFlag)   { flags += f }

    fun meta(block: (ItemMeta) -> Unit) { extraMeta += block }

    fun build(): ItemStack {
        val stack = ItemStack(material, amount.coerceIn(1, 64))
        val meta = stack.itemMeta ?: return stack

        displayName?.let { meta.setDisplayName(it) }

        if (loreLines.isNotEmpty()) meta.lore = loreLines.toList()

        flags.forEach { meta.addItemFlags(it) }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        // HIDE_ADDITIONAL_TOOLTIP added in 1.20.5 — guard against older builds
        runCatching { meta.addItemFlags(ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP")) }

        if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }

        customModelData?.let { meta.setCustomModelData(it) }
        extraMeta.forEach { it(meta) }

        stack.itemMeta = meta
        return stack
    }
}

// ── Common border items ───────────────────────────────────────────────────────

fun border(material: Material = Material.BLACK_STAINED_GLASS_PANE): ItemStack =
    itemStack(material) { name("§r") }

fun filler(material: Material = Material.GRAY_STAINED_GLASS_PANE): ItemStack =
    itemStack(material) { name("§r") }
