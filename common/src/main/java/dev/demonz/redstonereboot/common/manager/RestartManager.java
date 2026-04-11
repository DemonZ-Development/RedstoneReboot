package dev.demonz.redstonereboot.common.manager;

import dev.demonz.redstonereboot.common.platform.PlatformConfig;
import dev.demonz.redstonereboot.common.platform.ServerPlatform;
import dev.demonz.redstonereboot.common.schedule.RestartScheduleCalculator;
import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import dev.demonz.redstonereboot.common.scheduler.ScheduledTaskHandle;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared logic for managing scheduled, manual, and emergency restarts.
 */
public class RestartManager {

    private final Logger logger;
    private final ServerPlatform platform;
    private final PlatformTaskScheduler scheduler;
    private final PlatformConfig config;
    private final Supplier<ZonedDateTime> nowSupplier;

    private ScheduledTaskHandle currentRestartTask;
    private ScheduledTaskHandle schedulerTask;
    private volatile ZonedDateTime nextScheduledRestart;
    private volatile RestartReason currentRestartReason = RestartReason.UNKNOWN;
    private volatile String restartInitiator = "System";
    private final AtomicInteger secondsUntilRestart = new AtomicInteger(-1);

    public RestartManager(Logger logger, ServerPlatform platform, PlatformTaskScheduler scheduler, PlatformConfig config) {
        this(logger, platform, scheduler, config, () -> ZonedDateTime.now(config.getZoneId()));
    }

    RestartManager(
        Logger logger,
        ServerPlatform platform,
        PlatformTaskScheduler scheduler,
        PlatformConfig config,
        Supplier<ZonedDateTime> nowSupplier
    ) {
        this.logger = logger;
        this.platform = platform;
        this.scheduler = scheduler;
        this.config = config;
        this.nowSupplier = nowSupplier;
    }

    public void initialize() {
        scheduleRestarts();
        logger.info("RestartManager initialized - Timezone: " + config.getTimezone());
    }

    public void scheduleRestarts() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
        }

        if (!config.isScheduledRestartsEnabled()) {
            nextScheduledRestart = null;
            return;
        }

        calculateNextRestartTime();
        schedulerTask = scheduler.runRepeating(this::checkScheduledRestarts, 0L, 1200L);

        logger.info("Next restart: "
            + (nextScheduledRestart != null
            ? nextScheduledRestart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                + " " + config.getTimezone()
            : "None"));
    }

    private void calculateNextRestartTime() {
        nextScheduledRestart = RestartScheduleCalculator.calculateNextRestart(
            currentTime(),
            config.getScheduledTimes(),
            config.getScheduledDays()
        ).orElse(null);
    }

    private void checkScheduledRestarts() {
        if (nextScheduledRestart == null || isRestartInProgress()) {
            return;
        }

        ZonedDateTime now = currentTime();
        int warningTime = Math.max(config.getScheduledWarningTime(), 0);
        if (!now.isBefore(nextScheduledRestart.minusSeconds(warningTime))) {
            int remainingSeconds = (int) Math.max(0L, Duration.between(now, nextScheduledRestart).getSeconds());
            int countdownSeconds = Math.min(warningTime, remainingSeconds);

            scheduleRestart(countdownSeconds, RestartReason.SCHEDULED, "Scheduled System");
            calculateNextRestartTime();
        }
    }

    public synchronized boolean scheduleRestart(int delay, RestartReason reason, String initiator) {
        int normalizedDelay = Math.max(delay, 0);
        int currentRemaining = getSecondsUntilRestart();

        if (isRestartInProgress() && currentRemaining >= 0 && currentRemaining <= normalizedDelay) {
            logger.info("Ignoring restart request from " + initiator
                + " because a sooner restart is already running (" + currentRemaining + "s remaining).");
            return false;
        }

        if (isRestartInProgress()) {
            cancelCurrentCountdown(false);
            logger.warning("Replacing existing restart countdown with a sooner one (" + normalizedDelay + "s).");
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

    private synchronized void startCountdown(int seconds) {
        secondsUntilRestart.set(seconds);
        currentRestartTask = scheduler.runRepeating(() -> {
            synchronized (this) {
                int remaining = secondsUntilRestart.get();
                if (remaining <= 0) {
                    executeRestart();
                    return;
                }

                if (config.getWarningTimes().contains(remaining)) {
                    sendAlert(remaining);
                }
                secondsUntilRestart.decrementAndGet();
            }
        }, 0L, 20L);
        logger.info("Restart countdown: " + seconds + "s");
    }

    private void sendAlert(int seconds) {
        if (!config.isAlertsEnabled()) {
            return;
        }

        platform.sendRestartAlert(seconds, currentRestartReason);
    }

    private void executeRestart() {
        RestartReason reason = currentRestartReason;
        cancelCurrentCountdown(false);
        try {
            if (config.isAlertsEnabled()) {
                platform.sendFinalRestartAlert(reason);
            }
            platform.shutdownServer();
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Restart execution error", exception);
        }
    }

    public synchronized boolean cancelRestart() {
        if (!isRestartInProgress()) {
            return false;
        }

        cancelCurrentCountdown(true);
        return true;
    }

    private synchronized void cancelCurrentCountdown(boolean notify) {
        if (currentRestartTask != null) {
            currentRestartTask.cancel();
            currentRestartTask = null;
            if (notify && config.isAlertsEnabled()) {
                platform.sendRestartCancelledAlert();
            }
        }
        currentRestartReason = RestartReason.UNKNOWN;
        restartInitiator = "System";
        secondsUntilRestart.set(-1);
    }

    public synchronized boolean isRestartInProgress() {
        return currentRestartTask != null;
    }

    public synchronized int getSecondsUntilRestart() {
        return secondsUntilRestart.get();
    }

    public RestartReason getCurrentRestartReason() {
        return currentRestartReason;
    }

    public ZonedDateTime getNextScheduledRestart() {
        return nextScheduledRestart;
    }

    public synchronized void cleanup() {
        cancelCurrentCountdown(false);
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
        }
    }

    public synchronized Map<String, Object> getRestartInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("nextScheduledRestart", nextScheduledRestart);
        info.put("restartInProgress", isRestartInProgress());
        info.put("currentReason", currentRestartReason.getDisplayName());
        info.put("initiator", restartInitiator);
        info.put("timezone", config.getTimezone());
        info.put("secondsUntilRestart", secondsUntilRestart.get());
        return info;
    }

    private ZonedDateTime currentTime() {
        return nowSupplier.get();
    }
}
