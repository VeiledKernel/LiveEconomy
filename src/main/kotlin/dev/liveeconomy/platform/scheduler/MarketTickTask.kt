package dev.liveeconomy.platform.scheduler

import dev.liveeconomy.api.Lifecycle
import dev.liveeconomy.api.scheduler.TaskHandle
import dev.liveeconomy.core.market.MarketTicker
import dev.liveeconomy.data.config.MarketConfig

/**
 * Drives the market tick on a repeating async schedule.
 * Implements [Lifecycle] — must be started and stopped via the composition root.
 */
class MarketTickTask(
    private val ticker:    MarketTicker,
    private val scheduler: SchedulerImpl,
    private val config:    MarketConfig
) : Lifecycle {

    private var handle: TaskHandle? = null

    override fun start() {
        if (handle != null) return
        handle = scheduler.runRepeating(config.tickIntervalTicks) {
            ticker.tick()
        }
    }

    override fun stop() {
        handle?.cancel()
        handle = null
    }
}
