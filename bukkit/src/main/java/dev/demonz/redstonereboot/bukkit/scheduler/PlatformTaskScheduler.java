package dev.demonz.redstonereboot.bukkit.scheduler;

import org.bukkit.plugin.java.JavaPlugin;

public interface PlatformTaskScheduler {

    ScheduledTaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks);

    ScheduledTaskHandle runLater(Runnable task, long delayTicks);

    boolean isFolia();

    static PlatformTaskScheduler create(JavaPlugin plugin) {
        if (isFoliaEnvironment()) {
            return new FoliaTaskScheduler(plugin);
        }
        return new BukkitTaskScheduler(plugin);
    }

    static boolean isFoliaEnvironment() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
