package dev.demonz.redstonereboot.common.manager;

import dev.demonz.redstonereboot.common.backend.BackendRegistry;
import dev.demonz.redstonereboot.common.backend.BackendResult;
import dev.demonz.redstonereboot.common.backend.RestartBackend;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central manager for scheduling, counting down, and executing server restarts.
 * <p>
 * Handles scheduled restarts, manual restarts, emergency restarts, backend execution
 * with lockout protection, and player-facing countdown alerts. Thread-safe for
 * concurrent access from monitoring threads and command handlers.
 * </p>
 *
 * @see RestartReason
 * @see dev.demonz.redstonereboot.common.backend.BackendRegistry
 * @since 1.0.0
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
    private final BackendRegistry backendRegistry;
    private final AtomicBoolean controllerRestartPending = new AtomicBoolean(false);
    private long lockoutEndTime = 0;

    public RestartManager(Logger logger, ServerPlatform platform, PlatformTaskScheduler scheduler, PlatformConfig config, BackendRegistry backendRegistry) {
        this(logger, platform, scheduler, config, backendRegistry, () -> ZonedDateTime.now(config.getZoneId()));
    }

    RestartManager(
        Logger logger,
        ServerPlatform platform,
        PlatformTaskScheduler scheduler,
        PlatformConfig config,
        BackendRegistry backendRegistry,
        Supplier<ZonedDateTime> nowSupplier
    ) {
        this.logger = logger;
        this.platform = platform;
        this.scheduler = scheduler;
        this.config = config;
        this.backendRegistry = backendRegistry;
        this.nowSupplier = nowSupplier;
    }

    /**
     * Initialize the restart manager and start the scheduling loop.
     */
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

    /**
     * Schedule a restart with a countdown delay.
     * <p>
     * If a shorter restart is already in progress, this request is ignored.
     * If the backend is in lockout, the request is rejected.
     * </p>
     *
     * @param delay     countdown in seconds before the restart executes
     * @param reason    the reason for the restart
     * @param initiator identifier of who/what triggered the restart
     * @return {@code true} if the restart was accepted and scheduled
     */
    public synchronized boolean scheduleRestart(int delay, RestartReason reason, String initiator) {
        int normalizedDelay = Math.max(delay, 0);
        int currentRemaining = getSecondsUntilRestart();

        if (isRestartInProgress() && currentRemaining >= 0 && currentRemaining <= normalizedDelay) {
            logger.info("Ignoring restart request from " + initiator
                + " because a sooner restart is already running (" + currentRemaining + "s remaining).");
            return false;
        }

        if (isLockoutActive()) {
            logger.warning("Restart request from " + initiator + " blocked: Lockout state active.");
            return false;
        }

        if (controllerRestartPending.get()) {
            logger.warning("Restart request from " + initiator + " blocked: A controller-owned restart is already pending.");
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

    public synchronized void performImmediateRestart(RestartReason reason, String initiator) {
        if (isLockoutActive() || controllerRestartPending.get()) {
            logger.warning("Immediate restart blocked: Another restart is pending or lockout is active.");
            return;
        }

        cancelCurrentCountdown(false);
        this.currentRestartReason = reason;
        this.restartInitiator = initiator;
        executeRestart();
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
        if (controllerRestartPending.get()) return;

        RestartReason reason = currentRestartReason;
        RestartBackend backend = backendRegistry.getActiveBackend();
        
        cancelCurrentCountdown(false);

        try {
            // Phase 2: Handoff
            backend.prepare();
            BackendResult result = backend.execute();

            if (result == BackendResult.ACCEPTED) {
                if (backend.isControllerOwned()) {
                    if (config.isAlertsEnabled()) {
                        platform.sendFinalRestartAlert(reason);
                    }
                    controllerRestartPending.set(true);
                    logger.info("Restart accepted by Controller (" + backend.getName() + "). Local process ownership relinquished.");
                    
                    // Safety timeout: 5 minutes later, if we are still running, clear the flag.
                    scheduler.runLater(() -> {
                        if (controllerRestartPending.compareAndSet(true, false)) {
                            logger.warning("[Reboot] Safety timeout: Panel handoff duration exceeded. Relinquishing process ownership...");
                        }
                    }, 6000L); // 5 minutes (300 seconds * 20 ticks)
                } else {
                    // Supervisor-backed: Phase 3: Local Shutdown
                    if (config.isAlertsEnabled()) {
                        platform.sendFinalRestartAlert(reason);
                    }
                    platform.shutdownServer();
                }
            } else if (result == BackendResult.FAILED) {
                String detail = "Backend " + backend.getName() + " explicitly failed the restart request.";
                platform.sendPostponedAlert(detail);
                logger.severe("RESTART FAILED: " + detail);
            } else if (result == BackendResult.UNKNOWN) {
                int duration = backendRegistry.getConfig().getLockoutDuration();
                this.lockoutEndTime = System.currentTimeMillis() + (duration * 1000L);
                
                String detail = "Backend " + backend.getName() + " returned UNKNOWN status (Timeout?). Entering " + duration + "s lockout.";
                platform.sendPostponedAlert(detail);
                logger.warning("RESTART STATE UNKNOWN: " + detail);
            }

        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Restart execution error", exception);
            platform.sendPostponedAlert("Internal error during backend execution: " + exception.getMessage());
        }
    }

    public boolean isLockoutActive() {
        return System.currentTimeMillis() < lockoutEndTime;
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
