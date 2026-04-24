package dev.liveeconomy.platform.scheduler

import dev.liveeconomy.api.scheduler.Scheduler
import dev.liveeconomy.api.scheduler.TaskHandle
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

/**
 * Bukkit/Paper implementation of [Scheduler].
 *
 * The ONLY class that references [org.bukkit.scheduler.BukkitScheduler].
 * All scheduling in core/ and use cases goes through this interface.
 */
class SchedulerImpl(private val plugin: JavaPlugin) : Scheduler {

    override fun runAsync(task: () -> Unit) {
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable { task() })
    }

    override fun runOnMain(task: () -> Unit) {
        if (plugin.server.isPrimaryThread) task()
        else plugin.server.scheduler.runTask(plugin, Runnable { task() })
    }

    override fun runLater(delayTicks: Long, task: () -> Unit) {
        plugin.server.scheduler.runTaskLater(plugin, Runnable { task() }, delayTicks)
    }

    override fun runRepeating(intervalTicks: Long, task: () -> Unit): TaskHandle {
        val bukkit = plugin.server.scheduler.runTaskTimerAsynchronously(
            plugin, Runnable { task() }, intervalTicks, intervalTicks
        )
        return BukkitTaskHandle(bukkit)
    }

    override fun cancel(handle: TaskHandle) = handle.cancel()
}

private class BukkitTaskHandle(private val task: BukkitTask) : TaskHandle {
    override fun cancel() = task.cancel()
    override val isCancelled: Boolean get() = task.isCancelled
}
