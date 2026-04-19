package dev.liveeconomy.storage.yaml

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * Atomic YAML file writer.
 *
 * Writes to a `.tmp` file first, then atomically renames over the target.
 * On POSIX filesystems (Linux — standard Paper server OS), `renameTo` is
 * atomic at the OS level. A crash mid-write leaves the original file intact.
 *
 * **Known performance ceiling:** Every mutation triggers a full file rewrite.
 * This is acceptable for typical server order volumes (< 1000 open orders,
 * < 100 players). For high-frequency servers, introduce write debouncing
 * (50–200ms window) or a batch queue before each rewrite. This is a planned
 * Phase 5 optimisation — do not add it here without profiling first.
 *
 * **Multi-store atomicity:** YAML cannot guarantee atomicity across multiple
 * stores (e.g. wallet + order in one transaction). Each store write is
 * individually atomic, but cross-store consistency requires the SQL backend.
 * See [YamlTransactionScope] for the documented limitation.
 *
 * // No interface: stateless utility, never swapped
 */
internal object AtomicYamlWriter {

    /**
     * Save [yaml] to [target] atomically.
     * Creates parent directories if needed.
     */
    fun save(yaml: YamlConfiguration, target: File) {
        target.parentFile?.mkdirs()
        val tmp = File(target.parent, "${target.name}.tmp")
        yaml.save(tmp)
        if (!tmp.renameTo(target)) {
            // Fallback: cross-device rename failed — direct write
            System.err.println(
                "[AtomicYamlWriter] Atomic rename failed for ${target.name}, " +
                "falling back to direct write. Data may be at risk on crash."
            )
            yaml.save(target)
            tmp.delete()
        }
    }
}
