package dev.demonz.redstonereboot.folia;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;

/**
 * Folia-specific entry point that reuses the Bukkit implementation while
 * enabling the Folia metadata flag and scheduler bridge.
 *
 * <p>The Folia adaptation is handled entirely by
 * {@link dev.demonz.redstonereboot.bukkit.scheduler.BukkitSchedulerFactory}
 * (which detects the Folia runtime and selects the appropriate scheduler) and
 * {@link dev.demonz.redstonereboot.bukkit.scheduler.FoliaTaskScheduler}
 * (which bridges Folia's regionized scheduler API via reflection). This class
 * exists only to provide a separate {@code plugin.yml} with Folia-specific
 * metadata (e.g. the {@code folia-supported: true} flag) so that the plugin
 * loader recognises Folia compatibility. No additional logic is required here.</p>
 */
public final class RedstoneRebootFoliaPlugin extends RedstoneRebootPlugin {
}
