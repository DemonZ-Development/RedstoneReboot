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
    private final Method runDelayedMethod;
    private final Method runAtFixedRateMethod;

    FoliaTaskScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
        try {
            globalScheduler = plugin.getServer().getClass().getMethod("getGlobalRegionScheduler").invoke(plugin.getServer());
            runDelayedMethod = globalScheduler.getClass()
                .getMethod("runDelayed", org.bukkit.plugin.Plugin.class, Consumer.class, long.class);
            runAtFixedRateMethod = globalScheduler.getClass()
                .getMethod("runAtFixedRate", org.bukkit.plugin.Plugin.class, Consumer.class, long.class, long.class);
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
    public boolean isFolia() {
        return true;
    }

    private ScheduledTaskHandle reflectionHandle(Object scheduledTask) {
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
