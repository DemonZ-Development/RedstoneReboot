package dev.demonz.redstonereboot.bukkit.utils;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import dev.demonz.redstonereboot.common.manager.RestartReason;
import dev.demonz.redstonereboot.common.scheduler.ScheduledTaskHandle;

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
    private volatile boolean emergencyTpsTriggered;
    private volatile boolean emergencyMemoryTriggered;

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
        if (!plugin.getConfigManager().isMonitoringEnabled()) {
            consecutiveLowTPS = 0;
            return;
        }

        if (plugin.getRestartManager().isRestartInProgress()) {
            consecutiveLowTPS = 0;
            return;
        }

        double threshold = plugin.getConfigManager().getTpsThreshold();
        if (lastTPS < threshold) {
            consecutiveLowTPS++;
            if (consecutiveLowTPS >= plugin.getConfigManager().getConsecutiveChecks()) {
                triggerRestart(RestartReason.EMERGENCY_TPS, "ServerMonitor");
                consecutiveLowTPS = 0;
            }
        } else {
            consecutiveLowTPS = 0;
        }
    }

    private void checkMemory() {
        if (!plugin.getConfigManager().isMonitoringEnabled()) {
            consecutiveHighMemory = 0;
            return;
        }

        if (plugin.getRestartManager().isRestartInProgress()) {
            consecutiveHighMemory = 0;
            return;
        }

        double threshold = plugin.getConfigManager().getMemoryThreshold();
        if (lastMemoryUsage > threshold) {
            consecutiveHighMemory++;
            if (consecutiveHighMemory >= plugin.getConfigManager().getConsecutiveChecks()) {
                triggerRestart(RestartReason.EMERGENCY_MEMORY, "ServerMonitor");
                consecutiveHighMemory = 0;
            }
        } else {
            consecutiveHighMemory = 0;
        }
    }

    private void checkEmergency() {
        if (!plugin.getConfigManager().isEmergencyRestartEnabled()) {
            emergencyTpsTriggered = false;
            emergencyMemoryTriggered = false;
            return;
        }

        // Emergency conditions are allowed to shorten or replace an existing countdown.
        boolean triggered = false;

        if (lastTPS < plugin.getConfigManager().getEmergencyTpsThreshold()) {
            if (!emergencyTpsTriggered) {
                plugin.sendEmergencyAlert("Critical TPS: " + String.format("%.1f", lastTPS));
                triggerRestart(RestartReason.EMERGENCY_TPS, "EmergencyMonitor");
                emergencyTpsTriggered = true;
                triggered = true;
            }
        } else {
            emergencyTpsTriggered = false;
        }

        if (!triggered && lastMemoryUsage > plugin.getConfigManager().getEmergencyMemoryThreshold()) {
            if (!emergencyMemoryTriggered) {
                plugin.sendEmergencyAlert("Critical Memory: " + String.format("%.1f%%", lastMemoryUsage));
                triggerRestart(RestartReason.EMERGENCY_MEMORY, "EmergencyMonitor");
                emergencyMemoryTriggered = true;
            }
        } else if (!triggered) {
            emergencyMemoryTriggered = false;
        }
    }

    private void triggerRestart(RestartReason reason, String initiator) {
        int delay = plugin.getConfigManager().getEmergencyDelay();
        if (delay > 0) {
            plugin.getRestartManager().scheduleRestart(delay, reason, initiator);
        } else {
            plugin.getRestartManager().performImmediateRestart(reason, initiator);
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
