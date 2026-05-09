package dev.liveeconomy.arch

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File

/**
 * Architecture boundary tests — enforces dependency direction via source scan.
 *
 * Runs as part of `mvn test`. Fails CI if boundaries are violated.
 * No bytecode analysis required — plain import scanning.
 */
class ArchitectureBoundaryTest {

    private val srcRoot = File("src/main/kotlin/dev/liveeconomy")

    private fun filesIn(pkg: String): List<File> =
        File(srcRoot, pkg).walkTopDown().filter { it.extension == "kt" }.toList()

    private fun importsIn(files: List<File>): List<Pair<File, String>> =
        files.flatMap { file ->
            file.readLines()
                .filter { it.startsWith("import dev.liveeconomy.") }
                .map { file to it.trim() }
        }

    // ── Layer boundary rules ──────────────────────────────────────────────────

    @Test fun `core does not import integration`() {
        val violations = importsIn(filesIn("core"))
            .filter { (_, i) -> i.contains("dev.liveeconomy.integration") }
        if (violations.isNotEmpty()) fail("core/ imports integration.*:\n" +
            violations.joinToString("\n") { (f, i) -> "  ${f.name}: $i" })
    }

    @Test fun `core does not import gui or command`() {
        val violations = importsIn(filesIn("core"))
            .filter { (_, i) -> i.contains("dev.liveeconomy.gui") || i.contains("dev.liveeconomy.command") }
        if (violations.isNotEmpty()) fail("core/ imports gui or command:\n" +
            violations.joinToString("\n") { (f, i) -> "  ${f.name}: $i" })
    }

    @Test fun `storage does not import gui, command, or view`() {
        val violations = importsIn(filesIn("storage"))
            .filter { (_, i) -> listOf("dev.liveeconomy.gui", "dev.liveeconomy.command", "dev.liveeconomy.view")
                .any { i.contains(it) } }
        if (violations.isNotEmpty()) fail("storage/ imports gui/command/view:\n" +
            violations.joinToString("\n") { (f, i) -> "  ${f.name}: $i" })
    }

    @Test fun `api does not import core, storage, gui, command, platform, or integration`() {
        val violations = importsIn(filesIn("api"))
            .filter { (_, i) -> listOf("dev.liveeconomy.core", "dev.liveeconomy.storage",
                "dev.liveeconomy.gui", "dev.liveeconomy.command",
                "dev.liveeconomy.platform", "dev.liveeconomy.integration").any { i.contains(it) } }
        if (violations.isNotEmpty()) fail("api/ has outward imports:\n" +
            violations.joinToString("\n") { (f, i) -> "  ${f.name}: $i" })
    }

    @Test fun `gui does not import storage directly`() {
        val violations = importsIn(filesIn("gui"))
            .filter { (_, i) -> i.contains("dev.liveeconomy.storage") }
        if (violations.isNotEmpty()) fail("gui/ imports storage:\n" +
            violations.joinToString("\n") { (f, i) -> "  ${f.name}: $i" })
    }

    @Test fun `command does not import storage directly`() {
        val violations = importsIn(filesIn("command"))
            .filter { (_, i) -> i.contains("dev.liveeconomy.storage") }
        if (violations.isNotEmpty()) fail("command/ imports storage:\n" +
            violations.joinToString("\n") { (f, i) -> "  ${f.name}: $i" })
    }

    @Test fun `view does not import gui or command`() {
        val violations = importsIn(filesIn("view"))
            .filter { (_, i) -> i.contains("dev.liveeconomy.gui") || i.contains("dev.liveeconomy.command") }
        if (violations.isNotEmpty()) fail("view/ imports gui or command:\n" +
            violations.joinToString("\n") { (f, i) -> "  ${f.name}: $i" })
    }

    // ── Bukkit import enforcement ─────────────────────────────────────────────

    @Test fun `core does not import Bukkit entity or event classes`() {
        // Approved exceptions — v5.0 breaking API cleanup deferred:
        //   WalletServiceImpl: implements WalletService API which defines Player overloads
        //   TradeServiceImpl:  implements TradeService API which defines Player overloads
        // No other Bukkit imports are permitted in core/.
        val approvedFiles = setOf("WalletServiceImpl.kt", "TradeServiceImpl.kt")

        val violations = filesIn("core")
            .filter { it.name !in approvedFiles }
            .flatMap { file ->
                file.readLines()
                    .filter { it.startsWith("import org.bukkit") }
                    .map { file to it.trim() }
            }

        if (violations.isNotEmpty()) fail("core/ has unapproved Bukkit imports:\n" +
            violations.joinToString("\n") { (f, i) -> "  ${f.name}: $i" })
    }
}
