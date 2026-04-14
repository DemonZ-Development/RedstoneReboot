package dev.demonz.redstonereboot.bukkit.integrations;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import dev.demonz.redstonereboot.common.manager.RestartManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * PlaceholderAPI expansion for RedstoneReboot.
 * <p>
 * Provides 8 placeholders for use in scoreboards, tab lists, MOTD plugins, and chat.
 * All placeholders are null-safe and will return sensible defaults during early
 * server initialization and server-list MOTD pings.
 * </p>
 *
 * @since 1.0.0
 */
public class PlaceholderAPIHook extends PlaceholderExpansion {

    private static final DateTimeFormatter DATETIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RedstoneRebootPlugin plugin;

    public PlaceholderAPIHook(RedstoneRebootPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "redstonereboot";
    }

    @Override
    public @NotNull String getAuthor() {
        return "DemonZDevelopment";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // Guard against early-startup calls (MOTD pings before full initialization)
        RestartManager restartManager = safeGetRestartManager();

        switch (params.toLowerCase()) {
            case "next_restart": {
                if (restartManager == null) return "Not available";
                ZonedDateTime next = restartManager.getNextScheduledRestart();
                if (next == null) return "Not scheduled";
                String tz = safeGetTimezone();
                return next.format(DATETIME_FORMAT) + " " + tz;
            }

            case "time_until": {
                if (restartManager == null) return "N/A";
                ZonedDateTime next = restartManager.getNextScheduledRestart();
                if (next == null) return "N/A";
                ZonedDateTime now = ZonedDateTime.now(safeGetZoneId());
                if (!next.isAfter(now)) return "Soon";
                long mins = ChronoUnit.MINUTES.between(now, next);
                long h = mins / 60;
                long m = mins % 60;
                if (h > 0) return h + "h " + m + "m";
                if (m > 0) return m + "m";
                return "Soon";
            }

            case "status": {
                if (restartManager == null) return "Starting up";
                return restartManager.isRestartInProgress()
                    ? "Restart in progress"
                    : "Normal operation";
            }

            case "reason": {
                if (restartManager == null) return "None";
                if (!restartManager.isRestartInProgress()) return "None";
                return restartManager.getCurrentRestartReason() != null
                    ? restartManager.getCurrentRestartReason().getDisplayName()
                    : "None";
            }

            case "tps": {
                if (plugin.getServerLoadMonitor() == null) return "20.0";
                return String.format("%.1f", plugin.getServerLoadMonitor().getLastTPS());
            }

            case "memory": {
                if (plugin.getServerLoadMonitor() == null) return "0.0%";
                return String.format("%.1f%%", plugin.getServerLoadMonitor().getLastMemoryUsage());
            }

            case "version":
                return plugin.getDescription().getVersion();

            case "timezone":
                return safeGetTimezone();

            default:
                return null;
        }
    }

    /**
     * Safely get the restart manager, returning null if the core hasn't initialized yet.
     */
    private RestartManager safeGetRestartManager() {
        try {
            return plugin.getRestartManager();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Safely get the configured timezone string, falling back to UTC.
     */
    private String safeGetTimezone() {
        try {
            return plugin.getConfigManager() != null
                ? plugin.getConfigManager().getTimezone()
                : "UTC";
        } catch (Exception e) {
            return "UTC";
        }
    }

    /**
     * Safely get the configured ZoneId, falling back to UTC.
     */
    private java.time.ZoneId safeGetZoneId() {
        try {
            return plugin.getConfigManager() != null
                ? plugin.getConfigManager().getZoneId()
                : java.time.ZoneId.of("UTC");
        } catch (Exception e) {
            return java.time.ZoneId.of("UTC");
        }
    }
}
