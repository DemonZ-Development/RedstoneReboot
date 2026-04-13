package dev.demonz.redstonereboot.common.monitor;

import dev.demonz.redstonereboot.common.manager.RestartManager;
import dev.demonz.redstonereboot.common.manager.RestartReason;
import dev.demonz.redstonereboot.common.platform.PlatformConfig;
import dev.demonz.redstonereboot.common.platform.ServerPlatform;
import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import dev.demonz.redstonereboot.common.scheduler.ScheduledTaskHandle;

import java.util.logging.Logger;

/**
 * Shared health monitor for non-Bukkit platforms.
 */
public final class PlatformLoadMonitor {

    private final Logger logger;
    private final ServerPlatform platform;
    private final PlatformTaskScheduler scheduler;
    private final PlatformConfig config;
    private final RestartManager restartManager;

    private ScheduledTaskHandle monitorTask;
    private double lastTPS = 20.0D;
    private double lastMemoryUsage;
    private int consecutiveLowTPS;
    private int consecutiveHighMemory;
    private boolean emergencyTpsTriggered;
    private boolean emergencyMemoryTriggered;

    public PlatformLoadMonitor(
        Logger logger,
        ServerPlatform platform,
        PlatformTaskScheduler scheduler,
        PlatformConfig config,
        RestartManager restartManager
    ) {
        this.logger = logger;
        this.platform = platform;
        this.scheduler = scheduler;
        this.config = config;
        this.restartManager = restartManager;
    }

    public void startMonitoring() {
        stopMonitoring();
        long intervalTicks = Math.max(config.getCheckInterval(), 1) * 20L;
        monitorTask = scheduler.runRepeating(this::checkHealth, intervalTicks, intervalTicks);
        logger.info("Load monitoring active (interval: " + config.getCheckInterval() + "s)");
    }

    public void stopMonitoring() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
    }

    public double getLastTPS() {
        return lastTPS;
    }

    public double getLastMemoryUsage() {
        return lastMemoryUsage;
    }

    private void checkHealth() {
        lastTPS = platform.getTPS();

        Runtime runtime = Runtime.getRuntime();
        lastMemoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100.0D;

        checkTPS();
        checkMemory();
        checkEmergency();
    }

    private void checkTPS() {
        if (!config.isMonitoringEnabled()) {
            consecutiveLowTPS = 0;
            return;
        }

        if (restartManager.isRestartInProgress()) {
            consecutiveLowTPS = 0;
            return;
        }

        if (lastTPS < config.getTpsThreshold()) {
            consecutiveLowTPS++;
            if (consecutiveLowTPS >= config.getConsecutiveChecks()) {
                triggerRestart(RestartReason.EMERGENCY_TPS, "ServerMonitor");
                consecutiveLowTPS = 0;
            }
        } else {
            consecutiveLowTPS = 0;
        }
    }

    private void checkMemory() {
        if (!config.isMonitoringEnabled()) {
            consecutiveHighMemory = 0;
            return;
        }

        if (restartManager.isRestartInProgress()) {
            consecutiveHighMemory = 0;
            return;
        }

        if (lastMemoryUsage > config.getMemoryThreshold()) {
            consecutiveHighMemory++;
            if (consecutiveHighMemory >= config.getConsecutiveChecks()) {
                triggerRestart(RestartReason.EMERGENCY_MEMORY, "ServerMonitor");
                consecutiveHighMemory = 0;
            }
        } else {
            consecutiveHighMemory = 0;
        }
    }

    private void checkEmergency() {
        if (!config.isEmergencyRestartEnabled()) {
            emergencyTpsTriggered = false;
            emergencyMemoryTriggered = false;
            return;
        }

        // Emergency conditions are allowed to shorten or replace an existing countdown.
        boolean triggered = false;

        if (lastTPS < config.getEmergencyTpsThreshold()) {
            if (!emergencyTpsTriggered) {
                platform.sendEmergencyAlert("Critical TPS: " + String.format("%.1f", lastTPS));
                triggerRestart(RestartReason.EMERGENCY_TPS, "EmergencyMonitor");
                emergencyTpsTriggered = true;
                triggered = true;
            }
        } else {
            emergencyTpsTriggered = false;
        }

        if (!triggered && lastMemoryUsage > config.getEmergencyMemoryThreshold()) {
            if (!emergencyMemoryTriggered) {
                platform.sendEmergencyAlert("Critical Memory: " + String.format("%.1f%%", lastMemoryUsage));
                triggerRestart(RestartReason.EMERGENCY_MEMORY, "EmergencyMonitor");
                emergencyMemoryTriggered = true;
            }
        } else if (!triggered) {
            emergencyMemoryTriggered = false;
        }
    }

    private void triggerRestart(RestartReason reason, String initiator) {
        int delay = config.getEmergencyDelay();
        if (delay > 0) {
            restartManager.scheduleRestart(delay, reason, initiator);
        } else {
            restartManager.performImmediateRestart(reason, initiator);
        }
    }
}
