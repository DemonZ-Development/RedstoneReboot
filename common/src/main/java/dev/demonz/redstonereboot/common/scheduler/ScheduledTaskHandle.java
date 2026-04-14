package dev.demonz.redstonereboot.common.scheduler;

/**
 * Handle to a scheduled task that allows cancellation.
 * <p>
 * Returned by {@link PlatformTaskScheduler} when scheduling tasks.
 * Calling {@link #cancel()} is idempotent; cancelling an already-cancelled
 * or completed task has no effect.
 * </p>
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface ScheduledTaskHandle {

    /**
     * Cancel this scheduled task. If the task has already completed or been
     * cancelled, this method has no effect.
     */
    void cancel();
}
