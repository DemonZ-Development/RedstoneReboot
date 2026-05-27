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

    public static final int CURRENT_CONFIG_VERSION = 3;

    private final Plugin plugin;
    private volatile FileConfiguration config;

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
        validateConfiguration(this.config);
    }

    private void validateConfiguration(FileConfiguration cfg) {
        int version = cfg.getInt("config-version", CURRENT_CONFIG_VERSION);
        if (version < CURRENT_CONFIG_VERSION) {
            plugin.getLogger().warning("Your config.yml is outdated! (v" + version + " < v" + CURRENT_CONFIG_VERSION + ")");
        }

        String timezone = cfg.getString("scheduled-restarts.timezone", "UTC");
        try {
            ZoneId.of(timezone);
        } catch (Exception exception) {
            throw new RuntimeException("Invalid timezone '" + timezone + "'. Use a valid ZoneId like 'Europe/London' or 'UTC'.");
        }

        for (String time : cfg.getStringList("scheduled-restarts.times")) {
            if (!time.matches("^([0-1]?\\d|2[0-3]):[0-5]\\d$")) {
                throw new RuntimeException("Invalid time format '" + time + "'. Use HH:MM (24-hour).");
            }
        }

        Set<String> validDays = Set.of("ALL");
        Set<String> weekdayKeys = java.util.Arrays.stream(DayOfWeek.values())
            .map(Enum::name)
            .collect(Collectors.toSet());
        List<String> configuredDays = cfg.getStringList("scheduled-restarts.days");
        List<String> days = configuredDays.isEmpty() ? List.of("ALL") : configuredDays;
        for (String day : days) {
            String normalized = day.toUpperCase(Locale.ROOT);
            if (!validDays.contains(normalized) && !weekdayKeys.contains(normalized)) {
                throw new RuntimeException("Invalid day value '" + day + "'.");
            }
        }

        double tpsThreshold = cfg.getDouble("monitoring.tps-threshold", 18.0D);
        if (tpsThreshold < 0.0D || tpsThreshold > 20.0D) {
            throw new RuntimeException("TPS threshold must be between 0 and 20.");
        }
        double memThreshold = cfg.getDouble("monitoring.memory-threshold", 85.0D);
        if (memThreshold < 0.0D || memThreshold > 100.0D) {
            throw new RuntimeException("Memory threshold must be between 0 and 100.");
        }
        double emergTps = cfg.getDouble("emergency.tps-threshold", 12.0D);
        if (emergTps < 0.0D || emergTps > 20.0D) {
            throw new RuntimeException("Emergency TPS threshold must be between 0 and 20.");
        }
        double emergMem = cfg.getDouble("emergency.memory-threshold", 95.0D);
        if (emergMem < 0.0D || emergMem > 100.0D) {
            throw new RuntimeException("Emergency memory threshold must be between 0 and 100.");
        }
        if (cfg.getInt("monitoring.check-interval", 30) <= 0) {
            throw new RuntimeException("Monitoring check-interval must be greater than 0.");
        }
        if (cfg.getInt("monitoring.consecutive-checks", 3) <= 0) {
            throw new RuntimeException("Monitoring consecutive-checks must be greater than 0.");
        }
        if (cfg.getInt("emergency.delay", 30) < 0) {
            throw new RuntimeException("Emergency delay must not be negative.");
        }
        int permLevel = cfg.getInt("permissions.fallback.default-level", 2);
        if (permLevel < 0 || permLevel > 4) {
            throw new RuntimeException("default-permission-level must be 0-4 (got " + permLevel + ").");
        }
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        FileConfiguration newConfig = plugin.getConfig();
        if (isStrictValidationEnabled()) {
            // Validate the new config BEFORE assigning it to this.config.
            // If validation fails, the old config remains active.
            try {
                validateConfiguration(newConfig);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Config validation failed — keeping previous config. Error: " + e.getMessage());
                throw e;
            }
        }
        this.config = newConfig;
    }

    public int getConfigVersion() {
        return config.getInt("config-version", CURRENT_CONFIG_VERSION);
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
        return config.getBoolean("scheduled-restarts.enabled", true);
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
        return config.getBoolean("monitoring.enabled", true);
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
        return config.getBoolean("emergency.enabled", true);
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

    @Override
    public boolean isPublicPermissionsEnabled() {
        return config.getBoolean("permissions.fallback.public-permissions-enabled", true);
    }

    public boolean isMetricsEnabled() {
        return config.getBoolean("advanced.metrics-enabled", true);
    }

    /** @return the internal config (read-only — do not modify) */
    public FileConfiguration getRawConfig() {
        return config;
    }
}
