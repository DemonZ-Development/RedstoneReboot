package dev.demonz.redstonereboot.bukkit.scheduler;

import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import dev.demonz.redstonereboot.common.scheduler.ScheduledTaskHandle;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

final class BukkitTaskScheduler implements PlatformTaskScheduler {

    private final JavaPlugin plugin;

    BukkitTaskScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ScheduledTaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
        BukkitTask scheduled = Bukkit.getScheduler().runTaskTimer(
            plugin,
            () -> safelyRun(task),
            initialDelayTicks,
            periodTicks
        );
        return scheduled::cancel;
    }

    @Override
    public ScheduledTaskHandle runLater(Runnable task, long delayTicks) {
        BukkitTask scheduled = Bukkit.getScheduler().runTaskLater(plugin, () -> safelyRun(task), delayTicks);
        return scheduled::cancel;
    }

    @Override
    public boolean isFolia() {
        return false;
    }

    private void safelyRun(Runnable task) {
        try {
            task.run();
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Scheduled task failed.", exception);
        }
    }
}
