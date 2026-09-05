package dev.demonz.redstonereboot.common.manager;

import dev.demonz.redstonereboot.common.api.MessageContext;
import dev.demonz.redstonereboot.common.api.RedstoneRebootAPI;
import dev.demonz.redstonereboot.common.backend.BackendRegistry;
import dev.demonz.redstonereboot.common.backend.BackendResult;
import dev.demonz.redstonereboot.common.backend.RestartBackend;
import dev.demonz.redstonereboot.common.platform.PlatformConfig;
import dev.demonz.redstonereboot.common.platform.ServerPlatform;
import dev.demonz.redstonereboot.common.schedule.RestartScheduleCalculator;
import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import dev.demonz.redstonereboot.common.scheduler.ScheduledTaskHandle;

import java.nio.file.Path;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private final RestartHistory history;
    private final AtomicBoolean controllerRestartPending = new AtomicBoolean(false);
    private final AtomicBoolean restartExecuting = new AtomicBoolean(false);
    private final AtomicBoolean shutdownGuard = new AtomicBoolean(false);
    private final AtomicLong restartGeneration = new AtomicLong(0);
    private long countdownGeneration;
    private volatile long lockoutEndTime = 0;
    private volatile ScheduledTaskHandle controllerSafetyTask;

    public RestartManager(Logger logger, ServerPlatform platform, PlatformTaskScheduler scheduler, PlatformConfig config, BackendRegistry backendRegistry, Path dataFolder) {
        this(logger, platform, scheduler, config, backendRegistry, () -> ZonedDateTime.now(config.getZoneId()), dataFolder);
    }

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
        this(logger, platform, scheduler, config, backendRegistry, nowSupplier, null);
    }

    RestartManager(
        Logger logger,
        ServerPlatform platform,
        PlatformTaskScheduler scheduler,
        PlatformConfig config,
        BackendRegistry backendRegistry,
        Supplier<ZonedDateTime> nowSupplier,
        Path dataFolder
    ) {
        this.logger = logger;
        this.platform = platform;
        this.scheduler = scheduler;
        this.config = config;
        this.backendRegistry = backendRegistry;
        this.nowSupplier = nowSupplier;
        this.history = new RestartHistory(dataFolder, nowSupplier);
    }

    public void initialize() {
        scheduleRestarts();
        logger.info("RestartManager initialized - Timezone: " + config.getTimezone());
    }

    public synchronized void scheduleRestarts() {
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
            ? nextScheduledRestart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withLocale(Locale.ROOT))
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

    private synchronized void checkScheduledRestarts() {
        ZonedDateTime scheduled = nextScheduledRestart;
        if (scheduled == null || isRestartInProgress()) {
            return;
        }

        ZonedDateTime now = currentTime();
        int warningTime = Math.max(config.getScheduledWarningTime(), 0);
        if (!now.isBefore(scheduled.minusSeconds(warningTime))) {
            int remainingSeconds = (int) Math.max(0L, Duration.between(now, scheduled).getSeconds());
            int countdownSeconds = Math.min(warningTime, remainingSeconds);

            ZonedDateTime triggeredTime = scheduled;
            scheduleRestart(countdownSeconds, RestartReason.SCHEDULED, "Scheduled System");
            nextScheduledRestart = RestartScheduleCalculator.calculateNextRestart(
                triggeredTime.plusSeconds(1),
                config.getScheduledTimes(),
                config.getScheduledDays()
            ).orElse(null);
        }
    }

    public synchronized boolean scheduleRestart(int delay, RestartReason reason, String initiator) {
        if (restartExecuting.get()) return false;
        int normalizedDelay = Math.max(0, Math.min(delay, 63072000));
        long currentRemaining = getSecondsUntilRestart();

        if (isRestartInProgress() && currentRemaining >= 0 && currentRemaining <= normalizedDelay) {
            logger.warning("Ignoring restart request from " + initiator
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
        history.record("SCHEDULED", reason.getDisplayName(), initiator);
        try {
            RedstoneRebootAPI api = RedstoneRebootAPI.getInstance();
            if (api != null) api.fireScheduled(normalizedDelay, reason, initiator);
        } catch (Exception ignored) {}

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
        history.record("IMMEDIATE", reason.getDisplayName(), initiator);
        try {
            RedstoneRebootAPI api = RedstoneRebootAPI.getInstance();
            if (api != null) api.fireScheduled(0, reason, initiator);
        } catch (Exception ignored) {}
        executeRestart();
    }

    private synchronized void startCountdown(int seconds) {
        final long generation = ++countdownGeneration;
        secondsUntilRestart.set(seconds);
        currentRestartTask = scheduler.runRepeating(() -> {
            synchronized (this) {
                if (generation != countdownGeneration || shutdownGuard.get()) {
                    return;
                }
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
        if (!restartExecuting.compareAndSet(false, true)) {
            logger.warning("executeRestart called while already executing — blocked re-entrant call (possible GC upset)");
            return;
        }

        if (controllerRestartPending.get()) {
            restartExecuting.set(false);
            return;
        }

        final long currentGeneration = restartGeneration.incrementAndGet();
        RestartReason reason = currentRestartReason;
        String initiator = restartInitiator;
        RestartBackend backend = backendRegistry.getActiveBackend();

        cancelCurrentCountdown(false);

        try {
            scheduler.runLaterAsync(() -> {
                try {
                    if (shutdownGuard.get()) {
                        logger.warning("Async restart callback skipped — engine is shutting down.");
                        return;
                    }
                    backend.prepare();
                    BackendResult result = backend.execute();
                    try {
                        scheduler.runLater(() -> {
                            if (restartGeneration.get() != currentGeneration) {
                                logger.fine("Discarding stale restart result (generation mismatch).");
                                return;
                            }
                            handleExecutionResult(result, reason, backend, initiator);
                        }, 0);
                    } catch (Exception innerException) {
                        if (restartGeneration.get() != currentGeneration) {
                            logger.fine("Discarding stale restart result (generation mismatch, inline path).");
                            return;
                        }
                        logger.warning("Failed to dispatch result to main thread, cannot run inline due to thread safety limits: " + innerException.getMessage());
                    }
                } catch (Exception exception) {
                    try {
                        scheduler.runLater(() -> {
                            logger.log(Level.SEVERE, "Restart execution error", exception);
                            platform.sendPostponedAlert("Internal error during backend execution: " + exception.getMessage());
                        }, 0);
                    } catch (Exception innerException) {
                        logger.log(Level.SEVERE, "Failed to schedule restart execution error callback", innerException);
                    }
                } finally {
                    restartExecuting.set(false);
                }
            }, 0);
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Failed to submit async restart execution task", exception);
            restartExecuting.set(false);
        }
    }

    private void handleExecutionResult(BackendResult result, RestartReason reason, RestartBackend backend, String initiator) {
        if (result == BackendResult.ACCEPTED) {
            if (backend.isControllerOwned()) {
                if (config.isAlertsEnabled()) {
                    platform.sendFinalRestartAlert(reason);
                }
                controllerRestartPending.set(true);
                logger.info("Restart accepted by Controller (" + backend.getName() + "). Local process ownership relinquished.");
                history.record("EXECUTED", reason.getDisplayName(), initiator);
                try { RedstoneRebootAPI api = RedstoneRebootAPI.getInstance(); if (api != null) api.fireExecuting(reason, initiator); } catch (Exception ignored) {}

                synchronized (this) {
                    if (controllerSafetyTask != null) {
                        controllerSafetyTask.cancel();
                    }
                    controllerSafetyTask = scheduler.runLater(() -> {
                        if (controllerRestartPending.compareAndSet(true, false)) {
                            logger.warning("[Reboot] Safety timeout: Panel handoff duration exceeded. Relinquishing process ownership...");
                        }
                    }, 6000L);
                }
            } else {
                if (config.isAlertsEnabled()) {
                    platform.sendFinalRestartAlert(reason);
                }
                try { RedstoneRebootAPI api = RedstoneRebootAPI.getInstance(); if (api != null) api.fireExecuting(reason, initiator); } catch (Exception ignored) {}
                platform.shutdownServer(reason.getDisplayName());
                history.record("EXECUTED", reason.getDisplayName(), initiator);
            }
        } else if (result == BackendResult.FAILED) {
            String detail = "Backend " + backend.getName() + " explicitly failed the restart request.";
            platform.sendPostponedAlert(detail);
            logger.severe("RESTART FAILED: " + detail);
            history.record("POSTPONED", reason.getDisplayName(), initiator);
            try { RedstoneRebootAPI api = RedstoneRebootAPI.getInstance(); if (api != null) api.fireFailed(detail); } catch (Exception ignored) {}
        } else if (result == BackendResult.UNKNOWN) {
            int duration = backendRegistry.getConfig().getLockoutDuration();
            this.lockoutEndTime = System.currentTimeMillis() + (duration * 1000L);

            String detail = "Backend " + backend.getName() + " returned UNKNOWN status (Timeout?). Entering " + duration + "s lockout.";
            platform.sendPostponedAlert(detail);
            logger.warning("RESTART STATE UNKNOWN: " + detail);
            history.record("LOCKOUT", reason.getDisplayName(), initiator);
            try { RedstoneRebootAPI api = RedstoneRebootAPI.getInstance(); if (api != null) api.fireLockout(duration); } catch (Exception ignored) {}
        }
    }

    public boolean isLockoutActive() {
        return System.currentTimeMillis() < lockoutEndTime;
    }

    public synchronized boolean cancelRestart() {
        if (!isRestartInProgress() || restartExecuting.get()) {
            return false;
        }

        String reason = currentRestartReason.getDisplayName();
        String initiator = restartInitiator;
        cancelCurrentCountdown(true);

        if (controllerSafetyTask != null) {
            controllerSafetyTask.cancel();
            controllerSafetyTask = null;
        }
        controllerRestartPending.set(false);
        history.record("CANCELLED", reason, initiator);
        try { RedstoneRebootAPI api = RedstoneRebootAPI.getInstance(); if (api != null) api.fireCancelled(reason, initiator); } catch (Exception ignored) {}
        return true;
    }

    private synchronized void cancelCurrentCountdown(boolean notify) {
        countdownGeneration++;
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
        return currentRestartTask != null || restartExecuting.get();
    }

    public synchronized int getSecondsUntilRestart() {
        return secondsUntilRestart.get();
    }

    public RestartReason getCurrentRestartReason() {
        return currentRestartReason;
    }

    public boolean isControllerRestartPending() {
        return controllerRestartPending.get();
    }

    public String getRestartInitiator() {
        return restartInitiator;
    }

    public RestartHistory getHistory() {
        return history;
    }

    public ZonedDateTime getNextScheduledRestart() {
        return nextScheduledRestart;
    }

    public synchronized void cleanup() {
        shutdownGuard.set(true);
        if (isRestartInProgress()) {
            cancelCurrentCountdown(true);
            logger.info("Cleanup: cancelled in-progress restart.");
        }
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
            logger.info("Cleanup: stopped scheduled restart checks.");
        }
        if (controllerSafetyTask != null) {
            controllerSafetyTask.cancel();
            controllerSafetyTask = null;
        }
        controllerRestartPending.set(false);
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
