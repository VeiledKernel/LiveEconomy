package dev.liveeconomy.api.scheduler

/**
 * Platform-agnostic scheduling abstraction.
 *
 * Core services and Use Cases must never import Paper's scheduler directly
 * (Rule 14 — services are thread-agnostic). Instead, receive this interface
 * via constructor injection wherever async or delayed work is needed.
 *
 * Only [dev.liveeconomy.platform.scheduler.SchedulerImpl] may reference
 * Bukkit's scheduler — this is the single platform boundary.
 *
 * @since 4.0 (internal — not part of the public API surface)
 */
interface Scheduler {

    /**
     * Run [task] asynchronously off the main thread.
     * Do NOT touch Bukkit API inside [task] — use [runOnMain] for that.
     */
    fun runAsync(task: () -> Unit)

    /**
     * Run [task] on the main server thread on the next tick.
     * Use this to dispatch Bukkit API calls from async contexts.
     */
    fun runOnMain(task: () -> Unit)

    /**
     * Run [task] once after [delayTicks] server ticks.
     */
    fun runLater(delayTicks: Long, task: () -> Unit)

    /**
     * Schedule [task] to run repeatedly every [intervalTicks] server ticks.
     * Returns a [TaskHandle] that can cancel the task.
     */
    fun runRepeating(intervalTicks: Long, task: () -> Unit): TaskHandle

    /**
     * Cancel a previously scheduled repeating task.
     * Safe to call if the task is already cancelled.
     */
    fun cancel(handle: TaskHandle)
}

/** Opaque handle to a scheduled repeating task. */
interface TaskHandle {
    fun cancel()
    val isCancelled: Boolean
}
