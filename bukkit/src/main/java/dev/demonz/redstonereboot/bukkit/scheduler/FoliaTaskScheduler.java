package dev.demonz.redstonereboot.bukkit.scheduler;

import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import dev.demonz.redstonereboot.common.scheduler.ScheduledTaskHandle;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Platform adapter for Folia regionized server.
 */
public final class FoliaTaskScheduler implements PlatformTaskScheduler {

    private final JavaPlugin plugin;
    private final Object globalScheduler;
    private final Object asyncScheduler;
    private final Method runDelayedMethod;
    private final Method runAtFixedRateMethod;
    private final Method asyncRunNowMethod;
    private final Method asyncRunDelayedMethod;
    private final Method asyncRunAtFixedRateMethod;

    FoliaTaskScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
        try {
            Class<?> pluginClass = org.bukkit.plugin.Plugin.class;
            Class<?> consumerClass = Consumer.class;

            Object server = plugin.getServer();
            globalScheduler = server.getClass().getMethod("getGlobalRegionScheduler").invoke(server);
            Class<?> globalSchedulerClass = globalScheduler.getClass();
            runDelayedMethod = globalSchedulerClass
                .getMethod("runDelayed", pluginClass, consumerClass, long.class);
            runAtFixedRateMethod = globalSchedulerClass
                .getMethod("runAtFixedRate", pluginClass, consumerClass, long.class, long.class);

            asyncScheduler = server.getClass().getMethod("getAsyncScheduler").invoke(server);
            Class<?> asyncSchedulerClass = asyncScheduler.getClass();
            asyncRunNowMethod = asyncSchedulerClass
                .getMethod("runNow", pluginClass, consumerClass);
            asyncRunDelayedMethod = asyncSchedulerClass
                .getMethod("runDelayed", pluginClass, consumerClass, long.class);
            asyncRunAtFixedRateMethod = asyncSchedulerClass
                .getMethod("runAtFixedRate", pluginClass, consumerClass, long.class, long.class);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to initialize Folia scheduler bridge.", exception);
        }
    }

    @Override
    public ScheduledTaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
        try {
            Object scheduledTask = runAtFixedRateMethod.invoke(
                globalScheduler,
                plugin,
                (Consumer<Object>) ignored -> safelyRun(task),
                Math.max(1L, initialDelayTicks),
                periodTicks
            );
            return reflectionHandle(scheduledTask);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to schedule repeating Folia task.", exception);
        }
    }

    @Override
    public ScheduledTaskHandle runRepeatingAsync(Runnable task, long initialDelayTicks, long periodTicks) {
        try {
            Object scheduledTask = asyncRunAtFixedRateMethod.invoke(
                asyncScheduler,
                plugin,
                (Consumer<Object>) ignored -> safelyRun(task),
                initialDelayTicks,
                periodTicks
            );
            return reflectionHandle(scheduledTask);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to schedule repeating async Folia task.", exception);
        }
    }

    @Override
    public ScheduledTaskHandle runLater(Runnable task, long delayTicks) {
        try {
            Object scheduledTask = runDelayedMethod.invoke(
                globalScheduler,
                plugin,
                (Consumer<Object>) ignored -> safelyRun(task),
                delayTicks
            );
            return reflectionHandle(scheduledTask);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to schedule delayed Folia task.", exception);
        }
    }

    @Override
    public ScheduledTaskHandle runLaterAsync(Runnable task, long delayTicks) {
        if (delayTicks <= 0) {
            // AsyncScheduler.runNow does not accept a Consumer with 0 ticks — use runNow
            try {
                Object scheduledTask = asyncRunNowMethod.invoke(
                    asyncScheduler,
                    plugin,
                    (Consumer<Object>) ignored -> safelyRun(task)
                );
                return reflectionHandle(scheduledTask);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to schedule immediate async Folia task.", exception);
            }
        }
        try {
            Object scheduledTask = asyncRunDelayedMethod.invoke(
                asyncScheduler,
                plugin,
                (Consumer<Object>) ignored -> safelyRun(task),
                delayTicks
            );
            return reflectionHandle(scheduledTask);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to schedule delayed async Folia task.", exception);
        }
    }

    @Override
    public boolean isFolia() {
        return true;
    }

    private ScheduledTaskHandle reflectionHandle(Object scheduledTask) {
        if (scheduledTask == null) {
            return () -> {};
        }
        try {
            java.lang.reflect.Method cancelMethod = scheduledTask.getClass().getMethod("cancel");
            cancelMethod.setAccessible(true);
            return () -> {
                try {
                    cancelMethod.invoke(scheduledTask);
                } catch (ReflectiveOperationException exception) {
                    plugin.getLogger().log(Level.WARNING, "Failed to cancel Folia scheduled task.", exception);
                }
            };
        } catch (NoSuchMethodException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not find cancel method on Folia task.", exception);
            return () -> {};
        }
    }

    private void safelyRun(Runnable task) {
        try {
            task.run();
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Scheduled task failed.", exception);
        }
    }
}
