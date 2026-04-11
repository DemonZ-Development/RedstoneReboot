package dev.demonz.redstonereboot.bukkit.managers;

import dev.demonz.redstonereboot.common.platform.PlatformConfig;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages plugin configuration loading, validation, and access.
 */
public class ConfigManager implements PlatformConfig {

    public static final int CURRENT_CONFIG_VERSION = 2;

    private final Plugin plugin;
    private FileConfiguration config;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        if (isStrictValidationEnabled()) {
            validateConfiguration();
        }
    }

    private void validateConfiguration() {
        int version = getConfigVersion();
        if (version < CURRENT_CONFIG_VERSION) {
            plugin.getLogger().warning("Your config.yml is outdated! (v" + version + " < v" + CURRENT_CONFIG_VERSION + ")");
        }

        try {
            ZoneId.of(getTimezone());
        } catch (Exception exception) {
            throw new RuntimeException("Invalid timezone '" + getTimezone() + "'. Use a valid ZoneId like 'Asia/Kolkata' or 'UTC'.");
        }

        for (String time : getScheduledTimes()) {
            if (!time.matches("^([0-1]?\\d|2[0-3]):[0-5]\\d$")) {
                throw new RuntimeException("Invalid time format '" + time + "'. Use HH:MM (24-hour).");
            }
        }

        Set<String> validDays = Set.of("ALL");
        Set<String> weekdayKeys = java.util.Arrays.stream(DayOfWeek.values())
            .map(Enum::name)
            .collect(Collectors.toSet());
        for (String day : getScheduledDays()) {
            String normalized = day.toUpperCase(Locale.ROOT);
            if (!validDays.contains(normalized) && !weekdayKeys.contains(normalized)) {
                throw new RuntimeException("Invalid day value '" + day + "'.");
            }
        }

        if (getTpsThreshold() < 0.0D || getTpsThreshold() > 20.0D) {
            throw new RuntimeException("TPS threshold must be between 0 and 20.");
        }
        if (getMemoryThreshold() < 0.0D || getMemoryThreshold() > 100.0D) {
            throw new RuntimeException("Memory threshold must be between 0 and 100.");
        }
        if (getEmergencyTpsThreshold() < 0.0D || getEmergencyTpsThreshold() > 20.0D) {
            throw new RuntimeException("Emergency TPS threshold must be between 0 and 20.");
        }
        if (getEmergencyMemoryThreshold() < 0.0D || getEmergencyMemoryThreshold() > 100.0D) {
            throw new RuntimeException("Emergency memory threshold must be between 0 and 100.");
        }
        if (getCheckInterval() <= 0) {
            throw new RuntimeException("Monitoring check-interval must be greater than 0.");
        }
        if (getConsecutiveChecks() <= 0) {
            throw new RuntimeException("Monitoring consecutive-checks must be greater than 0.");
        }
        if (getEmergencyDelay() < 0) {
            throw new RuntimeException("Emergency delay must not be negative.");
        }
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        if (isStrictValidationEnabled()) {
            validateConfiguration();
        }
    }

    public int getConfigVersion() {
        return config.getInt("config-version", 1);
    }

    public String getPrefix() {
        return config.getString("general.plugin-prefix", "§8[§cRedstone§8] §aReboot");
    }

    public boolean isDebugMode() {
        return config.getBoolean("general.debug-mode", false);
    }

    public boolean isStrictValidationEnabled() {
        return config.getBoolean("general.strict-validation", true);
    }

    public boolean isScheduledRestartsEnabled() {
        return config.getBoolean("scheduled-restarts.enabled", false);
    }

    public List<String> getScheduledTimes() {
        return config.getStringList("scheduled-restarts.times");
    }

    public String getTimezone() {
        return config.getString("scheduled-restarts.timezone", "UTC");
    }

    public ZoneId getZoneId() {
        try {
            return ZoneId.of(getTimezone());
        } catch (Exception exception) {
            plugin.getLogger().warning("Invalid timezone '" + getTimezone() + "' in config. Falling back to UTC.");
            return ZoneId.of("UTC");
        }
    }

    public List<String> getScheduledDays() {
        List<String> configuredDays = config.getStringList("scheduled-restarts.days");
        return configuredDays.isEmpty() ? List.of("ALL") : configuredDays;
    }

    public int getScheduledWarningTime() {
        return Math.max(config.getInt("scheduled-restarts.warning-time", 300), 0);
    }

    public boolean isAlertsEnabled() {
        return config.getBoolean("alerts.enabled", true);
    }

    public List<Integer> getWarningTimes() {
        return config.getIntegerList("alerts.warning-times").stream()
            .filter(value -> value > 0)
            .distinct()
            .sorted(Comparator.reverseOrder())
            .toList();
    }

    public boolean isChatAlertsEnabled() {
        return config.getBoolean("alerts.chat.enabled", true);
    }

    public String getChatAlertFormat() {
        return config.getString("alerts.chat.format", "§8[§cRedstone§8] §eServer will restart in §c{time}§e!");
    }

    public boolean isTitleAlertsEnabled() {
        return config.getBoolean("alerts.title.enabled", true);
    }

    public String getTitleMainText() {
        return config.getString("alerts.title.main-title", "§cServer Restart");
    }

    public String getTitleSubText() {
        return config.getString("alerts.title.sub-title", "§ein §c{time}");
    }

    public boolean isActionBarAlertsEnabled() {
        return config.getBoolean("alerts.actionbar.enabled", true);
    }

    public String getActionBarFormat() {
        return config.getString("alerts.actionbar.format", "§8[§cRedstone§8] §eRestart in: §c{time}");
    }

    public boolean isSoundAlertsEnabled() {
        return config.getBoolean("alerts.sound.enabled", true);
    }

    public String getSoundName() {
        return config.getString("alerts.sound.sound-name", "BLOCK_NOTE_BLOCK_PLING");
    }

    public boolean isMonitoringEnabled() {
        return config.getBoolean("monitoring.enabled", false);
    }

    public double getTpsThreshold() {
        return config.getDouble("monitoring.tps-threshold", 18.0D);
    }

    public double getMemoryThreshold() {
        return config.getDouble("monitoring.memory-threshold", 85.0D);
    }

    public int getCheckInterval() {
        return Math.max(config.getInt("monitoring.check-interval", 30), 1);
    }

    public int getConsecutiveChecks() {
        return Math.max(config.getInt("monitoring.consecutive-checks", 3), 1);
    }

    public boolean isEmergencyRestartEnabled() {
        return config.getBoolean("emergency.enabled", false);
    }

    public double getEmergencyTpsThreshold() {
        return config.getDouble("emergency.tps-threshold", 12.0D);
    }

    public double getEmergencyMemoryThreshold() {
        return config.getDouble("emergency.memory-threshold", 95.0D);
    }

    public int getEmergencyDelay() {
        return Math.max(config.getInt("emergency.delay", 30), 0);
    }

    public int getShutdownDelayTicks() {
        return Math.max(config.getInt("advanced.shutdown-delay-ticks", 60), 0);
    }

    public boolean isLuckPermsIntegrationEnabled() {
        return config.getBoolean("permissions.luckperms.integration-enabled", true);
    }

    public boolean isUseOpAsAdminEnabled() {
        return config.getBoolean("permissions.fallback.use-op-as-admin", true);
    }

    public boolean isPlaceholderAPIEnabled() {
        return config.getBoolean("placeholders.enabled", true);
    }

    @Override
    public int getDefaultPermissionLevel() {
        return config.getInt("permissions.fallback.default-level", 2);
    }

    public FileConfiguration getRawConfig() {
        return config;
    }
}
