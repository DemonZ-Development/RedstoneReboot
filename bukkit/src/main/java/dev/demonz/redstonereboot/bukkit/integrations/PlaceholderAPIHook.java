/*
 * Copyright (c) 2026 DemonZ Development
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package dev.demonz.redstonereboot.bukkit.integrations;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import dev.demonz.redstonereboot.common.RedstoneRebootCore;
import dev.demonz.redstonereboot.common.manager.RestartManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

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
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withLocale(Locale.ROOT);

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
        return plugin != null && plugin.getDescription() != null
            ? plugin.getDescription().getVersion()
            : RedstoneRebootCore.VERSION;
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
                double tps = plugin != null ? plugin.getTPS() : 20.0;
                return String.format(Locale.ROOT, "%.1f", tps);
            }

            case "memory": {
                double memoryUsage = plugin != null ? plugin.getCachedMemoryUsage() : 0.0;
                return String.format(Locale.ROOT, "%.1f%%", memoryUsage);
            }

            case "version":
                return plugin != null && plugin.getDescription() != null
                    ? plugin.getDescription().getVersion()
                    : RedstoneRebootCore.VERSION;

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
        if (plugin == null) return null;
        try {
            return plugin.getRestartManager();
        } catch (Exception e) {
            plugin.getLogger().fine("Could not get RestartManager: " + e.getMessage());
            return null;
        }
    }

    /**
     * Safely get the configured timezone string, falling back to UTC.
     */
    private String safeGetTimezone() {
        if (plugin == null) return "UTC";
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
        if (plugin == null) return java.time.ZoneId.of("UTC");
        try {
            return plugin.getConfigManager() != null
                ? plugin.getConfigManager().getZoneId()
                : java.time.ZoneId.of("UTC");
        } catch (Exception e) {
            return java.time.ZoneId.of("UTC");
        }
    }
}