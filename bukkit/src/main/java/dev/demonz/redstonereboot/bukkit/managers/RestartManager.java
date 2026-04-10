package dev.demonz.redstonereboot.bukkit.managers;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import dev.demonz.redstonereboot.bukkit.events.RestartEvent;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages scheduled, manual, and emergency restarts.
 */
public class RestartManager {
    private final RedstoneRebootPlugin plugin;
    private final ConfigManager configManager;
    private final AlertManager alertManager;
    private final Map<String, BukkitTask> scheduledTasks = new ConcurrentHashMap<>();
    private BukkitTask currentRestartTask = null;
    private BukkitTask schedulerTask = null;
    private LocalDateTime nextScheduledRestart = null;
    private RestartReason currentRestartReason = RestartReason.UNKNOWN;
    private String restartInitiator = "System";

    public enum RestartReason {
        SCHEDULED("Scheduled Restart"),
        MANUAL("Manual Restart"),
        EMERGENCY_TPS("Emergency — Low TPS"),
        EMERGENCY_MEMORY("Emergency — High Memory"),
        API("API Restart"),
        UNKNOWN("Unknown");

        private final String displayName;
        RestartReason(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public RestartManager(RedstoneRebootPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.alertManager = plugin.getAlertManager();
    }

    public void initialize() {
        if (configManager.isScheduledRestartsEnabled()) scheduleRestarts();
        plugin.getLogger().info("RestartManager initialized — Timezone: " + configManager.getTimezone());
    }

    public void scheduleRestarts() {
        if (schedulerTask != null) schedulerTask.cancel();
        if (!configManager.isScheduledRestartsEnabled()) return;
        List<String> times = configManager.getScheduledTimes();
        if (times.isEmpty()) { plugin.getLogger().warning("No restart times configured!"); return; }
        calculateNextRestartTime();
        schedulerTask = new BukkitRunnable() {
            @Override public void run() { checkScheduledRestarts(); }
        }.runTaskTimer(plugin, 0L, 1200L);
        plugin.getLogger().info("Next restart: " +
            (nextScheduledRestart != null
                ? nextScheduledRestart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " " + configManager.getTimezone()
                : "None"));
    }

    private void calculateNextRestartTime() {
        List<String> times = configManager.getScheduledTimes();
        List<String> days = configManager.getScheduledDays();
        ZoneId zone = configManager.getZoneId();
        if (times.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime next = null;
        if (shouldRestartOnDay(now.getDayOfWeek().toString(), days)) {
            for (String t : times) {
                LocalTime lt = parseTime(t);
                if (lt != null) {
                    LocalDateTime dt = now.toLocalDate().atTime(lt);
                    if (dt.isAfter(now) && (next == null || dt.isBefore(next))) next = dt;
                }
            }
        }
        if (next == null) {
            for (int i = 1; i <= 7; i++) {
                LocalDateTime check = now.plusDays(i);
                if (shouldRestartOnDay(check.getDayOfWeek().toString(), days)) {
                    for (String t : times) {
                        LocalTime lt = parseTime(t);
                        if (lt != null) {
                            LocalDateTime dt = check.toLocalDate().atTime(lt);
                            if (next == null || dt.isBefore(next)) next = dt;
                        }
                    }
                    if (next != null) break;
                }
            }
        }
        this.nextScheduledRestart = next;
    }

    private void checkScheduledRestarts() {
        if (nextScheduledRestart == null || isRestartInProgress()) return;
        LocalDateTime now = LocalDateTime.now(configManager.getZoneId());
        if (now.isAfter(nextScheduledRestart.minusMinutes(1)) && now.isBefore(nextScheduledRestart.plusMinutes(1))) {
            scheduleRestart(configManager.getScheduledWarningTime(), RestartReason.SCHEDULED, "Scheduled System");
            calculateNextRestartTime();
        }
    }

    private LocalTime parseTime(String t) {
        try {
            String[] p = t.split(":");
            return LocalTime.of(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
        } catch (Exception e) { return null; }
    }

    private boolean shouldRestartOnDay(String day, List<String> configured) {
        return configured.contains("ALL") || configured.contains(day.toUpperCase());
    }

    public void scheduleRestart(int delay, RestartReason reason, String initiator) {
        this.currentRestartReason = reason;
        this.restartInitiator = initiator;
        RestartEvent event = new RestartEvent(reason, initiator, delay);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) { plugin.getLogger().info("Restart cancelled by plugin: " + event.getCancellationReason()); return; }
        if (delay <= 0) { executeRestart(); return; }
        startCountdown(delay);
    }

    private void startCountdown(int seconds) {
        if (currentRestartTask != null) currentRestartTask.cancel();
        currentRestartTask = new BukkitRunnable() {
            int remaining = seconds;
            @Override public void run() {
                if (remaining <= 0) { executeRestart(); cancel(); return; }
                if (configManager.getWarningTimes().contains(remaining))
                    alertManager.sendRestartAlert(remaining, currentRestartReason);
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        plugin.getLogger().info("Restart countdown: " + seconds + "s");
    }

    private void executeRestart() {
        try {
            alertManager.sendFinalRestartAlert(currentRestartReason);
            Bukkit.savePlayers();
            Bukkit.getWorlds().forEach(w -> { try { w.save(); } catch (Exception ignored) {} });
            plugin.getLogger().info("Data saved. Shutting down...");
            plugin.shutdownServer();
        } catch (Exception e) {
            plugin.getLogger().severe("Restart execution error: " + e.getMessage());
        } finally {
            currentRestartTask = null;
        }
    }

    public boolean cancelRestart() {
        if (currentRestartTask != null) {
            currentRestartTask.cancel();
            currentRestartTask = null;
            alertManager.sendRestartCancelledAlert();
            return true;
        }
        return false;
    }

    public LocalDateTime getNextScheduledRestart() { return nextScheduledRestart; }
    public boolean isRestartInProgress() { return currentRestartTask != null; }
    public RestartReason getCurrentRestartReason() { return currentRestartReason; }
    public String getRestartInitiator() { return restartInitiator; }

    public void cleanup() {
        scheduledTasks.values().forEach(BukkitTask::cancel);
        scheduledTasks.clear();
        if (currentRestartTask != null) { currentRestartTask.cancel(); currentRestartTask = null; }
        if (schedulerTask != null) { schedulerTask.cancel(); schedulerTask = null; }
    }

    public Map<String, Object> getRestartInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("nextScheduledRestart", nextScheduledRestart);
        info.put("restartInProgress", isRestartInProgress());
        info.put("currentReason", currentRestartReason.getDisplayName());
        info.put("initiator", restartInitiator);
        info.put("timezone", configManager.getTimezone());
        return info;
    }
}
