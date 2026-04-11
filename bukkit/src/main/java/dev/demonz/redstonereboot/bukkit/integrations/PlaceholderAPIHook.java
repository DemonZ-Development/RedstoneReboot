package dev.demonz.redstonereboot.bukkit.integrations;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import dev.demonz.redstonereboot.common.manager.RestartReason;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for RedstoneReboot.
 */
public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final RedstoneRebootPlugin plugin;

    public PlaceholderAPIHook(RedstoneRebootPlugin plugin) { this.plugin = plugin; }

    @Override public @NotNull String getIdentifier() { return "redstonereboot"; }
    @Override public @NotNull String getAuthor() { return "DemonZDevelopment"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        switch (params.toLowerCase()) {
            case "next_restart":
                java.time.ZonedDateTime next = plugin.getRestartManager().getNextScheduledRestart();
                return next != null
                    ? next.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " " + plugin.getConfigManager().getTimezone()
                    : "Not scheduled";

            case "time_until":
                java.time.ZonedDateTime n = plugin.getRestartManager().getNextScheduledRestart();
                if (n != null) {
                    java.time.ZonedDateTime now = java.time.ZonedDateTime.now(plugin.getConfigManager().getZoneId());
                    if (n.isAfter(now)) {
                        long mins = java.time.temporal.ChronoUnit.MINUTES.between(now, n);
                        long h = mins / 60, m = mins % 60;
                        return h > 0 ? h + "h " + m + "m" : m > 0 ? m + "m" : "Soon";
                    }
                }
                return "N/A";

            case "status":
                return plugin.getRestartManager() != null && plugin.getRestartManager().isRestartInProgress() ? "Restart in progress" : "Normal operation";

            case "reason":
                return plugin.getRestartManager() != null
                    && plugin.getRestartManager().isRestartInProgress()
                    && plugin.getRestartManager().getCurrentRestartReason() != null
                    ? plugin.getRestartManager().getCurrentRestartReason().getDisplayName()
                    : "None";

            case "tps":
                return plugin.getServerLoadMonitor() != null
                    ? String.format("%.1f", plugin.getServerLoadMonitor().getLastTPS()) : "20.0";

            case "memory":
                return plugin.getServerLoadMonitor() != null
                    ? String.format("%.1f%%", plugin.getServerLoadMonitor().getLastMemoryUsage()) : "0.0%";

            case "version":
                return plugin.getDescription().getVersion();

            case "timezone":
                return plugin.getConfigManager() != null ? plugin.getConfigManager().getTimezone() : "UTC";

            default: return null;
        }
    }

    // isRegistered() is final in PlaceholderExpansion, no need to override
}
