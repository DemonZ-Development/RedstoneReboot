package dev.demonz.redstonereboot.common.monitor;

import dev.demonz.redstonereboot.common.manager.RestartManager;
import dev.demonz.redstonereboot.common.manager.RestartReason;
import dev.demonz.redstonereboot.common.platform.PlatformConfig;
import dev.demonz.redstonereboot.common.platform.ServerPlatform;
import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import dev.demonz.redstonereboot.common.scheduler.ScheduledTaskHandle;

import java.util.Locale;
import java.util.logging.Logger;

public final class PlatformLoadMonitor {

    private final Logger logger;
    private final ServerPlatform platform;
    private final PlatformTaskScheduler scheduler;
    private final PlatformConfig config;
    private final RestartManager restartManager;

    private volatile ScheduledTaskHandle monitorTask;
    private volatile double lastTPS = 20.0D;
    private volatile double lastMemoryUsage;
    private volatile int consecutiveLowTPS;
    private volatile int consecutiveHighMemory;
    private final java.util.concurrent.atomic.AtomicBoolean emergencyTpsTriggered = new java.util.concurrent.atomic.AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicBoolean emergencyMemoryTriggered = new java.util.concurrent.atomic.AtomicBoolean(false);

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

    public synchronized void startMonitoring() {
        stopMonitoring();
        long intervalTicks = Math.max(config.getCheckInterval(), 1) * 20L;
        monitorTask = scheduler.runRepeating(this::checkHealth, intervalTicks, intervalTicks);
        logger.info("Load monitoring active (interval: " + config.getCheckInterval() + "s)");
    }

    public synchronized void stopMonitoring() {
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

        if (restartManager == null || restartManager.isRestartInProgress()) {
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

        if (restartManager == null || restartManager.isRestartInProgress()) {
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
            emergencyTpsTriggered.set(false);
            emergencyMemoryTriggered.set(false);
            return;
        }

        boolean tpsTriggeredThisTick = false;

        if (lastTPS >= 0 && lastTPS < config.getEmergencyTpsThreshold()) {
            if (emergencyTpsTriggered.compareAndSet(false, true)) {
                platform.sendEmergencyAlert("Critical TPS: " + String.format(Locale.ROOT, "%.1f", lastTPS));
                triggerRestart(RestartReason.EMERGENCY_TPS, "EmergencyMonitor");
                tpsTriggeredThisTick = true;
            } else {
                tpsTriggeredThisTick = true;
            }
        } else {
            emergencyTpsTriggered.set(false);
        }

        if (lastMemoryUsage > config.getEmergencyMemoryThreshold()) {
            if (!tpsTriggeredThisTick && emergencyMemoryTriggered.compareAndSet(false, true)) {
                platform.sendEmergencyAlert("Critical Memory: " + String.format(Locale.ROOT, "%.1f%%", lastMemoryUsage));
                triggerRestart(RestartReason.EMERGENCY_MEMORY, "EmergencyMonitor");
            }
        } else {
            emergencyMemoryTriggered.set(false);
        }
    }

    private void triggerRestart(RestartReason reason, String initiator) {
        int delay = config.getEmergencyDelay();
        if (restartManager == null) {
            return;
        }
        if (delay > 0) {
            restartManager.scheduleRestart(delay, reason, initiator);
        } else {
            restartManager.performImmediateRestart(reason, initiator);
        }
    }
}
