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

    /** No-op handle returned when scheduling fails due to reflection errors. */
    private static final ScheduledTaskHandle NO_OP_HANDLE = () -> {};

    private final JavaPlugin plugin;
    private final Object globalScheduler;
    private final Object asyncScheduler;
    private final Method runDelayedMethod;
    private final Method runAtFixedRateMethod;
    private final Method asyncRunNowMethod;
    private final Method asyncRunDelayedMethod;
    private final Method asyncRunAtFixedRateMethod;
    private final Method cancelMethod;

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

            // Resolve cancel method from returned task type
            cancelMethod = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask")
                .getMethod("cancel");
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
                initialDelayTicks,
                periodTicks
            );
            return reflectionHandle(scheduledTask);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to schedule repeating Folia task.", exception);
            return NO_OP_HANDLE;
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
            plugin.getLogger().log(Level.SEVERE, "Failed to schedule repeating async Folia task.", exception);
            return NO_OP_HANDLE;
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
            plugin.getLogger().log(Level.SEVERE, "Failed to schedule delayed Folia task.", exception);
            return NO_OP_HANDLE;
        }
    }

    @Override
    public ScheduledTaskHandle runLaterAsync(Runnable task, long delayTicks) {
        if (delayTicks <= 0) {
            try {
                Object scheduledTask = asyncRunNowMethod.invoke(
                    asyncScheduler,
                    plugin,
                    (Consumer<Object>) ignored -> safelyRun(task)
                );
                return reflectionHandle(scheduledTask);
            } catch (ReflectiveOperationException exception) {
                plugin.getLogger().log(Level.SEVERE, "Failed to schedule immediate async Folia task.", exception);
                return NO_OP_HANDLE;
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
            plugin.getLogger().log(Level.SEVERE, "Failed to schedule delayed async Folia task.", exception);
            return NO_OP_HANDLE;
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
        return () -> {
            try {
                cancelMethod.invoke(scheduledTask);
            } catch (ReflectiveOperationException exception) {
                plugin.getLogger().log(Level.WARNING, "Failed to cancel Folia scheduled task.", exception);
            }
        };
    }

    private void safelyRun(Runnable task) {
        try {
            task.run();
        } catch (Error error) {
            plugin.getLogger().log(Level.SEVERE, "Scheduled task threw an Error.", error);
            throw error;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Scheduled task failed.", exception);
        }
    }
}
