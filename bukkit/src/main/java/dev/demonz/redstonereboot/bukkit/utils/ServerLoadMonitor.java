package dev.demonz.redstonereboot.bukkit.utils;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import dev.demonz.redstonereboot.common.manager.RestartReason;
import dev.demonz.redstonereboot.common.scheduler.ScheduledTaskHandle;
import java.util.Locale;

/**
 * Real-time TPS and memory monitoring with automatic restart triggers.
 */
public class ServerLoadMonitor {

    private final RedstoneRebootPlugin plugin;

    private volatile ScheduledTaskHandle monitorTask;
    private volatile double lastTPS = 20.0D;
    private volatile double lastMemoryUsage;
    private volatile int consecutiveLowTPS;
    private volatile int consecutiveHighMemory;
    private final java.util.concurrent.atomic.AtomicBoolean emergencyTpsTriggered = new java.util.concurrent.atomic.AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicBoolean emergencyMemoryTriggered = new java.util.concurrent.atomic.AtomicBoolean(false);

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
        lastMemoryUsage = getMemoryUsagePercent();

        checkTPS();
        checkMemory();
        checkEmergency();
    }

    private void checkTPS() {
        if (!plugin.getConfigManager().isMonitoringEnabled() || lastTPS < 0) {
            consecutiveLowTPS = 0;
            return;
        }

        dev.demonz.redstonereboot.common.manager.RestartManager rm = plugin.getRestartManager();
        if (rm == null || rm.isRestartInProgress()) {
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

        dev.demonz.redstonereboot.common.manager.RestartManager rm = plugin.getRestartManager();
        if (rm == null || rm.isRestartInProgress()) {
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
            emergencyTpsTriggered.set(false);
            emergencyMemoryTriggered.set(false);
            return;
        }

        boolean triggered = false;

        if (lastTPS >= 0 && lastTPS < plugin.getConfigManager().getEmergencyTpsThreshold()) {
            if (emergencyTpsTriggered.compareAndSet(false, true)) {
                plugin.sendEmergencyAlert("Critical TPS: " + String.format(Locale.ROOT, "%.1f", lastTPS));
                triggerRestart(RestartReason.EMERGENCY_TPS, "EmergencyMonitor");
                triggered = true;
            }
        } else {
            emergencyTpsTriggered.set(false);
        }

        if (!triggered && lastMemoryUsage > plugin.getConfigManager().getEmergencyMemoryThreshold()) {
            if (emergencyMemoryTriggered.compareAndSet(false, true)) {
                plugin.sendEmergencyAlert("Critical Memory: " + String.format(Locale.ROOT, "%.1f%%", lastMemoryUsage));
                triggerRestart(RestartReason.EMERGENCY_MEMORY, "EmergencyMonitor");
            }
        } else if (!triggered) {
            emergencyMemoryTriggered.set(false);
        }
    }

    private void triggerRestart(RestartReason reason, String initiator) {
        int delay = plugin.getConfigManager().getEmergencyDelay();
        dev.demonz.redstonereboot.common.manager.RestartManager rm = plugin.getRestartManager();
        if (rm == null) {
            return;
        }
        if (delay > 0) {
            rm.scheduleRestart(delay, reason, initiator);
        } else {
            rm.performImmediateRestart(reason, initiator);
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

    /**
     * Calculate the current JVM memory usage as a percentage.
     * This is a shared utility to avoid duplicating the calculation across the codebase.
     *
     * @return memory usage percentage (0.0 – 100.0)
     */
    public static double getMemoryUsagePercent() {
        Runtime runtime = Runtime.getRuntime();
        return (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100.0D;
    }
}
