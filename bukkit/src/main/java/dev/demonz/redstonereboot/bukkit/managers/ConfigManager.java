package dev.demonz.redstonereboot.bukkit.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Manages plugin configuration loading, validation, and access.
 */
public class ConfigManager {
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

    public static final int CURRENT_CONFIG_VERSION = 1;

    private void validateConfiguration() {
        try {
            // Check config version
            int version = getConfigVersion();
            if (version < CURRENT_CONFIG_VERSION) {
                plugin.getLogger().warning("==========================================");
                plugin.getLogger().warning("Your config.yml is outdated! (v" + version + " < v" + CURRENT_CONFIG_VERSION + ")");
                plugin.getLogger().warning("Please backup and regenerate your config to get the latest features.");
                plugin.getLogger().warning("==========================================");
            }

            // Validate timezone
            String timezone = getTimezone();
            try {
                ZoneId.of(timezone);
            } catch (Exception e) {
                throw new RuntimeException("Invalid timezone '" + timezone + "'. Use a valid ZoneId like 'Asia/Kolkata' or 'UTC'");
            }

            // Validate scheduled restart times
            List<String> times = getScheduledTimes();
            for (String time : times) {
                if (!time.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                    throw new RuntimeException("Invalid time format '" + time + "'. Use HH:MM (24-hour)");
                }
            }

            // Validate thresholds
            double tps = getTpsThreshold();
            if (tps < 0 || tps > 20) throw new RuntimeException("TPS threshold must be 0-20, got: " + tps);
            double mem = getMemoryThreshold();
            if (mem < 0 || mem > 100) throw new RuntimeException("Memory threshold must be 0-100, got: " + mem);

            plugin.getLogger().info("Configuration validated successfully.");
        } catch (Exception e) {
            plugin.getLogger().severe("CONFIG VALIDATION FAILED: " + e.getMessage());
            throw new RuntimeException("Config validation failed: " + e.getMessage(), e);
        }
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        if (isStrictValidationEnabled()) validateConfiguration();
    }

    // ── General ──────────────────────────────────────────────────
    public int getConfigVersion() { return config.getInt("config-version", 1); }
    public String getPrefix() { return config.getString("general.plugin-prefix", "§8[§cRedstone§8] §aReboot"); }
    public boolean isDebugMode() { return config.getBoolean("general.debug-mode", false); }
    public boolean isStrictValidationEnabled() { return config.getBoolean("general.strict-validation", true); }

    // ── Scheduling ───────────────────────────────────────────────
    public boolean isScheduledRestartsEnabled() { return config.getBoolean("scheduled-restarts.enabled", false); }
    public List<String> getScheduledTimes() { return config.getStringList("scheduled-restarts.times"); }
    public String getTimezone() { return config.getString("scheduled-restarts.timezone", "UTC"); }
    public ZoneId getZoneId() {
        try { return ZoneId.of(getTimezone()); }
        catch (Exception e) { return ZoneId.of("UTC"); }
    }
    public List<String> getScheduledDays() { return config.getStringList("scheduled-restarts.days"); }
    public int getScheduledWarningTime() { return config.getInt("scheduled-restarts.warning-time", 300); }

    // ── Alerts ───────────────────────────────────────────────────
    public boolean isAlertsEnabled() { return config.getBoolean("alerts.enabled", true); }
    public List<Integer> getWarningTimes() { return config.getIntegerList("alerts.warning-times"); }
    public boolean isChatAlertsEnabled() { return config.getBoolean("alerts.chat.enabled", true); }
    public String getChatAlertFormat() { return config.getString("alerts.chat.format", "§8[§cRedstone§8] §eServer will restart in §c{time}§e!"); }
    public boolean isTitleAlertsEnabled() { return config.getBoolean("alerts.title.enabled", true); }
    public String getTitleMainText() { return config.getString("alerts.title.main-title", "§c⚡ Server Restart"); }
    public String getTitleSubText() { return config.getString("alerts.title.sub-title", "§ein §c{time}"); }
    public boolean isActionBarAlertsEnabled() { return config.getBoolean("alerts.actionbar.enabled", true); }
    public String getActionBarFormat() { return config.getString("alerts.actionbar.format", "§8[§cRedstone§8] §eRestart in: §c{time}"); }
    public boolean isSoundAlertsEnabled() { return config.getBoolean("alerts.sound.enabled", true); }
    public String getSoundName() { return config.getString("alerts.sound.sound-name", "BLOCK_NOTE_BLOCK_PLING"); }

    // ── Monitoring ───────────────────────────────────────────────
    public boolean isMonitoringEnabled() { return config.getBoolean("monitoring.enabled", false); }
    public double getTpsThreshold() { return config.getDouble("monitoring.tps-threshold", 18.0); }
    public double getMemoryThreshold() { return config.getDouble("monitoring.memory-threshold", 85.0); }
    public int getCheckInterval() { return config.getInt("monitoring.check-interval", 30); }
    public int getConsecutiveChecks() { return config.getInt("monitoring.consecutive-checks", 3); }

    // ── Emergency ────────────────────────────────────────────────
    public boolean isEmergencyRestartEnabled() { return config.getBoolean("emergency.enabled", false); }
    public double getEmergencyTpsThreshold() { return config.getDouble("emergency.tps-threshold", 12.0); }
    public double getEmergencyMemoryThreshold() { return config.getDouble("emergency.memory-threshold", 95.0); }
    public int getEmergencyDelay() { return config.getInt("emergency.delay", 30); }

    // ── Permissions ──────────────────────────────────────────────
    public boolean isLuckPermsIntegrationEnabled() { return config.getBoolean("permissions.luckperms.integration-enabled", true); }

    // ── PlaceholderAPI ───────────────────────────────────────────
    public boolean isPlaceholderAPIEnabled() { return config.getBoolean("placeholders.enabled", true); }

    public FileConfiguration getRawConfig() { return config; }
}
