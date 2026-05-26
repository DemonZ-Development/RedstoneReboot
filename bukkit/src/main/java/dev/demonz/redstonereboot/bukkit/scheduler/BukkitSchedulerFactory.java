package dev.demonz.redstonereboot.bukkit.scheduler;

import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitSchedulerFactory {

    private static Boolean foliaEnvironment;

    public static PlatformTaskScheduler create(JavaPlugin plugin) {
        if (isFoliaEnvironment()) {
            return new FoliaTaskScheduler(plugin);
        }
        return new BukkitTaskScheduler(plugin);
    }

    public static boolean isFoliaEnvironment() {
        if (foliaEnvironment != null) return foliaEnvironment;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            foliaEnvironment = true;
            return true;
        } catch (ClassNotFoundException exception) {
            foliaEnvironment = false;
            return false;
        }
    }
}
