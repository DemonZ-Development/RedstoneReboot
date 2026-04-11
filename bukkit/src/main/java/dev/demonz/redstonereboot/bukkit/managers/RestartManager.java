package dev.demonz.redstonereboot.bukkit.managers;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import dev.demonz.redstonereboot.bukkit.events.RestartEvent;
import dev.demonz.redstonereboot.bukkit.scheduler.ScheduledTaskHandle;
import dev.demonz.redstonereboot.common.schedule.RestartScheduleCalculator;
import org.bukkit.Bukkit;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages scheduled, manual, and emergency restarts.
 */
public class RestartManager {

    private final RedstoneRebootPlugin plugin;
    private final ConfigManager configManager;
    private final AlertManager alertManager;

    private ScheduledTaskHandle currentRestartTask;
    private ScheduledTaskHandle schedulerTask;
    private LocalDateTime nextScheduledRestart;
    private RestartReason currentRestartReason = RestartReason.UNKNOWN;
    private String restartInitiator = "System";
    private int secondsUntilRestart = -1;

    public enum RestartReason {
        SCHEDULED("Scheduled Restart"),
        MANUAL("Manual Restart"),
        EMERGENCY_TPS("Emergency - Low TPS"),
        EMERGENCY_MEMORY("Emergency - High Memory"),
        API("API Restart"),
        UNKNOWN("Unknown");

        private final String displayName;

        RestartReason(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public RestartManager(RedstoneRebootPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.alertManager = plugin.getAlertManager();
    }

    public void initialize() {
        scheduleRestarts();
        plugin.getLogger().info("RestartManager initialized - Timezone: " + configManager.getTimezone());
    }

    public void scheduleRestarts() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
        }

        if (!configManager.isScheduledRestartsEnabled()) {
            nextScheduledRestart = null;
            return;
        }

        calculateNextRestartTime();
        schedulerTask = plugin.getTaskScheduler().runRepeating(this::checkScheduledRestarts, 0L, 1200L);

        plugin.getLogger().info("Next restart: "
            + (nextScheduledRestart != null
            ? nextScheduledRestart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                + " " + configManager.getTimezone()
            : "None"));
    }

    private void calculateNextRestartTime() {
        nextScheduledRestart = RestartScheduleCalculator.calculateNextRestart(
            ZonedDateTime.now(configManager.getZoneId()),
            configManager.getScheduledTimes(),
            configManager.getScheduledDays()
        ).map(ZonedDateTime::toLocalDateTime).orElse(null);
    }

    private void checkScheduledRestarts() {
        if (nextScheduledRestart == null || isRestartInProgress()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(configManager.getZoneId());
        if (!now.isBefore(nextScheduledRestart.minusSeconds(30))) {
            scheduleRestart(configManager.getScheduledWarningTime(), RestartReason.SCHEDULED, "Scheduled System");
            calculateNextRestartTime();
        }
    }

    public boolean scheduleRestart(int delay, RestartReason reason, String initiator) {
        int normalizedDelay = Math.max(delay, 0);
        int currentRemaining = getSecondsUntilRestart();

        if (isRestartInProgress() && currentRemaining >= 0 && currentRemaining <= normalizedDelay) {
            plugin.getLogger().info("Ignoring restart request from " + initiator
                + " because a sooner restart is already running (" + currentRemaining + "s remaining).");
            return false;
        }

        RestartEvent event = new RestartEvent(reason, initiator, normalizedDelay);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            plugin.getLogger().info("Restart cancelled by plugin: " + event.getCancellationReason());
            return false;
        }

        if (isRestartInProgress()) {
            cancelCurrentCountdown(false);
            plugin.getLogger().warning("Replacing existing restart countdown with a sooner one (" + normalizedDelay + "s).");
        }

        currentRestartReason = reason;
        restartInitiator = initiator;

        if (normalizedDelay == 0) {
            executeRestart();
            return true;
        }

        startCountdown(normalizedDelay);
        return true;
    }

    private void startCountdown(int seconds) {
        cancelCurrentCountdown(false);
        secondsUntilRestart = seconds;
        currentRestartTask = plugin.getTaskScheduler().runRepeating(() -> {
            if (secondsUntilRestart <= 0) {
                executeRestart();
                return;
            }

            if (configManager.getWarningTimes().contains(secondsUntilRestart)) {
                alertManager.sendRestartAlert(secondsUntilRestart, currentRestartReason);
            }
            secondsUntilRestart--;
        }, 0L, 20L);
        plugin.getLogger().info("Restart countdown: " + seconds + "s");
    }

    private void executeRestart() {
        cancelCurrentCountdown(false);
        try {
            alertManager.sendFinalRestartAlert(currentRestartReason);
            Bukkit.savePlayers();
            Bukkit.getWorlds().forEach(world -> {
                try {
                    world.save();
                } catch (Exception ignored) {
                }
            });
            plugin.getLogger().info("Data saved. Shutting down...");
            plugin.shutdownServer();
        } catch (Exception exception) {
            plugin.getLogger().severe("Restart execution error: " + exception.getMessage());
        }
    }

    public boolean cancelRestart() {
        if (!isRestartInProgress()) {
            return false;
        }

        cancelCurrentCountdown(true);
        return true;
    }

    private void cancelCurrentCountdown(boolean notifyPlayers) {
        if (currentRestartTask != null) {
            currentRestartTask.cancel();
            currentRestartTask = null;
        }
        secondsUntilRestart = -1;
        if (notifyPlayers) {
            alertManager.sendRestartCancelledAlert();
        }
    }

    public LocalDateTime getNextScheduledRestart() {
        return nextScheduledRestart;
    }

    public boolean isRestartInProgress() {
        return currentRestartTask != null;
    }

    public int getSecondsUntilRestart() {
        return secondsUntilRestart;
    }

    public RestartReason getCurrentRestartReason() {
        return currentRestartReason;
    }

    public String getRestartInitiator() {
        return restartInitiator;
    }

    public void cleanup() {
        cancelCurrentCountdown(false);
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
        }
    }

    public Map<String, Object> getRestartInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("nextScheduledRestart", nextScheduledRestart);
        info.put("restartInProgress", isRestartInProgress());
        info.put("currentReason", currentRestartReason.getDisplayName());
        info.put("initiator", restartInitiator);
        info.put("timezone", configManager.getTimezone());
        info.put("secondsUntilRestart", secondsUntilRestart);
        return info;
    }
}
