package dev.demonz.redstonereboot.common.scheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Standard Java-based scheduler for platforms without a native tick-based scheduler (Fabric/Forge).
 */
public class JavaPlatformScheduler implements PlatformTaskScheduler {

    private static final Logger LOGGER = Logger.getLogger(JavaPlatformScheduler.class.getName());

    private final ScheduledExecutorService executor;
    private final Executor dispatcher;

    public JavaPlatformScheduler() {
        this(Runnable::run);
    }

    public JavaPlatformScheduler(Executor dispatcher) {
        this.dispatcher = dispatcher != null ? dispatcher : Runnable::run;
        this.executor = Executors.newSingleThreadScheduledExecutor(new SchedulerThreadFactory());
    }

    @Override
    public ScheduledTaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
        // Assuming 20 ticks per second (50ms per tick)
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(
            () -> dispatchSafely(task),
            initialDelayTicks * 50,
            periodTicks * 50,
            TimeUnit.MILLISECONDS
        );
        return () -> future.cancel(false);
    }

    @Override
    public ScheduledTaskHandle runLater(Runnable task, long delayTicks) {
        ScheduledFuture<?> future = executor.schedule(
            () -> dispatchSafely(task),
            delayTicks * 50,
            TimeUnit.MILLISECONDS
        );
        return () -> future.cancel(false);
    }

    @Override
    public boolean isFolia() {
        return false;
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private void dispatchSafely(Runnable task) {
        try {
            dispatcher.execute(() -> runSafely(task));
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to dispatch scheduled task.", exception);
        }
    }

    private void runSafely(Runnable task) {
        try {
            task.run();
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Scheduled task failed.", exception);
        }
    }

    private static final class SchedulerThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "RedstoneReboot-Scheduler");
            thread.setDaemon(true);
            return thread;
        }
    }
}
