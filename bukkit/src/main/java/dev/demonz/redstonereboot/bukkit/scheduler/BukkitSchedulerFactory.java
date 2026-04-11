package dev.demonz.redstonereboot.bukkit.scheduler;

import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitSchedulerFactory {

    public static PlatformTaskScheduler create(JavaPlugin plugin) {
        if (isFoliaEnvironment()) {
            return new FoliaTaskScheduler(plugin);
        }
        return new BukkitTaskScheduler(plugin);
    }

    public static boolean isFoliaEnvironment() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
