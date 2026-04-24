package dev.liveeconomy.platform.scheduler

import dev.liveeconomy.api.Lifecycle
import dev.liveeconomy.api.scheduler.TaskHandle
import dev.liveeconomy.api.storage.StorageProvider
import dev.liveeconomy.core.market.MarketRegistry
import dev.liveeconomy.data.config.MarketConfig

/**
 * Periodically flushes all prices and storage to disk.
 * Runs async — storage implementations are thread-safe.
 */
class AutoSaveTask(
    private val storage:   StorageProvider,
    private val registry:  MarketRegistry,
    private val scheduler: SchedulerImpl,
    private val config:    MarketConfig
) : Lifecycle {

    private var handle: TaskHandle? = null

    override fun start() {
        if (handle != null) return
        val intervalTicks = config.decayIntervalMinutes * 60 * 20
        handle = scheduler.runRepeating(intervalTicks) {
            registry.persistAllPrices()
        }
    }

    override fun stop() {
        handle?.cancel()
        handle = null
        // Final save on shutdown
        registry.persistAllPrices()
    }
}
