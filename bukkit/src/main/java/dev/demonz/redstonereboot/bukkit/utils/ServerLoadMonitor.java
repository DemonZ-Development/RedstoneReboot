package dev.demonz.redstonereboot.bukkit.utils;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import dev.demonz.redstonereboot.bukkit.managers.RestartManager;
import dev.demonz.redstonereboot.bukkit.scheduler.ScheduledTaskHandle;

/**
 * Real-time TPS and memory monitoring with automatic restart triggers.
 */
public class ServerLoadMonitor {

    private final RedstoneRebootPlugin plugin;

    private ScheduledTaskHandle monitorTask;
    private double lastTPS = 20.0D;
    private double lastMemoryUsage;
    private int consecutiveLowTPS;
    private int consecutiveHighMemory;
    private boolean emergencyTpsTriggered;
    private boolean emergencyMemoryTriggered;

    public ServerLoadMonitor(RedstoneRebootPlugin plugin) {
        this.plugin = plugin;
    }

    public void startMonitoring() {
        stopMonitoring();
        long intervalTicks = plugin.getConfigManager().getCheckInterval() * 20L;
        monitorTask = plugin.getTaskScheduler().runRepeating(this::checkHealth, intervalTicks, intervalTicks);
        plugin.getLogger().info("Load monitoring active (interval: "
            + plugin.getConfigManager().getCheckInterval() + "s)");
    }

    public void stopMonitoring() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
    }

    private void checkHealth() {
        lastTPS = plugin.getTPS();

        Runtime runtime = Runtime.getRuntime();
        lastMemoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100.0D;

        checkTPS();
        checkMemory();
        checkEmergency();
    }

    private void checkTPS() {
        if (plugin.getRestartManager().isRestartInProgress()) {
            consecutiveLowTPS = 0;
            return;
        }

        double threshold = plugin.getConfigManager().getTpsThreshold();
        if (lastTPS < threshold) {
            consecutiveLowTPS++;
            if (consecutiveLowTPS >= plugin.getConfigManager().getConsecutiveChecks()) {
                boolean scheduled = plugin.getRestartManager().scheduleRestart(
                    plugin.getConfigManager().getEmergencyDelay(),
                    RestartManager.RestartReason.EMERGENCY_TPS,
                    "ServerMonitor"
                );
                if (scheduled) {
                    consecutiveLowTPS = 0;
                }
            }
        } else {
            consecutiveLowTPS = 0;
        }
    }

    private void checkMemory() {
        if (plugin.getRestartManager().isRestartInProgress()) {
            consecutiveHighMemory = 0;
            return;
        }

        double threshold = plugin.getConfigManager().getMemoryThreshold();
        if (lastMemoryUsage > threshold) {
            consecutiveHighMemory++;
            if (consecutiveHighMemory >= plugin.getConfigManager().getConsecutiveChecks()) {
                boolean scheduled = plugin.getRestartManager().scheduleRestart(
                    plugin.getConfigManager().getEmergencyDelay(),
                    RestartManager.RestartReason.EMERGENCY_MEMORY,
                    "ServerMonitor"
                );
                if (scheduled) {
                    consecutiveHighMemory = 0;
                }
            }
        } else {
            consecutiveHighMemory = 0;
        }
    }

    private void checkEmergency() {
        if (!plugin.getConfigManager().isEmergencyRestartEnabled() || plugin.getRestartManager().isRestartInProgress()) {
            return;
        }

        if (lastTPS < plugin.getConfigManager().getEmergencyTpsThreshold()) {
            if (!emergencyTpsTriggered) {
                plugin.getAlertManager().sendEmergencyAlert("Critical TPS: " + String.format("%.1f", lastTPS));
                plugin.getRestartManager().scheduleRestart(
                    plugin.getConfigManager().getEmergencyDelay(),
                    RestartManager.RestartReason.EMERGENCY_TPS,
                    "EmergencyMonitor"
                );
                emergencyTpsTriggered = true;
            }
        } else {
            emergencyTpsTriggered = false;
        }

        if (lastMemoryUsage > plugin.getConfigManager().getEmergencyMemoryThreshold()) {
            if (!emergencyMemoryTriggered) {
                plugin.getAlertManager().sendEmergencyAlert("Critical Memory: " + String.format("%.1f%%", lastMemoryUsage));
                plugin.getRestartManager().scheduleRestart(
                    plugin.getConfigManager().getEmergencyDelay(),
                    RestartManager.RestartReason.EMERGENCY_MEMORY,
                    "EmergencyMonitor"
                );
                emergencyMemoryTriggered = true;
            }
        } else {
            emergencyMemoryTriggered = false;
        }
    }

    public double getLastTPS() {
        return lastTPS;
    }

    public double getLastMemoryUsage() {
        return lastMemoryUsage;
    }

    public boolean isHealthy() {
        return lastTPS >= plugin.getConfigManager().getTpsThreshold()
            && lastMemoryUsage <= plugin.getConfigManager().getMemoryThreshold();
    }
}
