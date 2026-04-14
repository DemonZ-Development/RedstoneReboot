package dev.demonz.redstonereboot.common.scheduler;

/**
 * Abstracts platform-specific task scheduling for the RedstoneReboot core engine.
 * <p>
 * Each platform (Bukkit, Folia, Fabric, Forge, NeoForge) provides an implementation
 * to bridge its native scheduler into the common engine's tick-based timing model.
 * All delay and period values are expressed in <b>server ticks</b> (1 tick = 50ms at 20 TPS).
 * </p>
 *
 * @see ScheduledTaskHandle
 * @since 1.0.0
 */
public interface PlatformTaskScheduler {

    /**
     * Schedule a task to run repeatedly at a fixed interval.
     *
     * @param task              the runnable to execute
     * @param initialDelayTicks initial delay before the first execution, in server ticks
     * @param periodTicks       interval between subsequent executions, in server ticks
     * @return a handle that can cancel the scheduled task
     */
    ScheduledTaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks);

    /**
     * Schedule a task to run once after a delay.
     *
     * @param task       the runnable to execute
     * @param delayTicks delay before execution, in server ticks
     * @return a handle that can cancel the scheduled task
     */
    ScheduledTaskHandle runLater(Runnable task, long delayTicks);

    /**
     * Check whether the current runtime is a Folia environment.
     *
     * @return {@code true} if running on a Folia-based server
     */
    boolean isFolia();
}
