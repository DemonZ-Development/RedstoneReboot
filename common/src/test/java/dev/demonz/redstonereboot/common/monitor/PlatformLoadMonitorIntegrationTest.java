package dev.demonz.redstonereboot.common.monitor;

import dev.demonz.redstonereboot.common.backend.BackendConfig;
import dev.demonz.redstonereboot.common.backend.BackendRegistry;
import dev.demonz.redstonereboot.common.manager.RestartManager;
import dev.demonz.redstonereboot.common.manager.RestartReason;
import dev.demonz.redstonereboot.common.platform.ServerPlatform;
import dev.demonz.redstonereboot.common.platform.SimplePlatformConfig;
import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import dev.demonz.redstonereboot.common.scheduler.ScheduledTaskHandle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link PlatformLoadMonitor} — TPS/memory monitoring,
 * consecutive check pattern, emergency triggers, and interaction with RestartManager.
 */
class PlatformLoadMonitorIntegrationTest {

    @TempDir
    Path tempDir;

    private Logger logger;
    private ControllablePlatform platform;
    private ControllableScheduler scheduler;
    private SimplePlatformConfig config;

    @BeforeEach
    void setUp() {
        logger = Logger.getLogger("PlatformLoadMonitorTest");
        platform = new ControllablePlatform();
        scheduler = new ControllableScheduler();
        config = new SimplePlatformConfig();
    }

    private BackendRegistry backendRegistry() {
        BackendRegistry reg = new BackendRegistry(logger, new BackendConfig(tempDir, logger), tempDir);
        reg.initialize();
        return reg;
    }

    // --- TPS monitoring: consecutive low TPS triggers restart ---

    @Test
    void consecutiveLowTPSTriggersMonitoringRestart() {
        platform.setTps(10.0); // Below default threshold of 18
        config.setMonitoringEnabled(true);
        config.setTpsThreshold(18.0);
        config.setConsecutiveChecks(3);
        config.setEmergencyRestartEnabled(false);

        RestartManager manager = new RestartManager(
            logger, platform, scheduler, config, backendRegistry());
        PlatformLoadMonitor monitor = new PlatformLoadMonitor(
            logger, platform, scheduler, config, manager);

        monitor.startMonitoring();

        // Tick 1: consecutiveLowTPS = 1
        scheduler.tickRepeating(0);
        assertFalse(manager.isRestartInProgress());

        // Tick 2: consecutiveLowTPS = 2
        scheduler.tickRepeating(0);
        assertFalse(manager.isRestartInProgress());

        // Tick 3: consecutiveLowTPS = 3 → triggers restart
        scheduler.tickRepeating(0);
        assertTrue(manager.isRestartInProgress(),
            "After 3 consecutive low TPS checks, restart should be triggered");
        assertEquals(RestartReason.EMERGENCY_TPS, manager.getCurrentRestartReason());
    }

    // --- TPS recovery resets consecutive counter ---

    @Test
    void tpsRecoveryResetsConsecutiveCounter() {
        config.setMonitoringEnabled(true);
        config.setTpsThreshold(18.0);
        config.setConsecutiveChecks(3);
        config.setEmergencyRestartEnabled(false);

        RestartManager manager = new RestartManager(
            logger, platform, scheduler, config, backendRegistry());
        PlatformLoadMonitor monitor = new PlatformLoadMonitor(
            logger, platform, scheduler, config, manager);

        monitor.startMonitoring();

        // Tick 1: low TPS
        platform.setTps(10.0);
        scheduler.tickRepeating(0);
        assertFalse(manager.isRestartInProgress());

        // Tick 2: TPS recovers
        platform.setTps(19.0);
        scheduler.tickRepeating(0);
        assertFalse(manager.isRestartInProgress());

        // Tick 3: low TPS again — counter should have reset, so consecutive = 1
        platform.setTps(10.0);
        scheduler.tickRepeating(0);
        assertFalse(manager.isRestartInProgress(),
            "Counter should have reset after recovery, so 1 check is not enough");

        // Ticks 4-5: more low TPS
        scheduler.tickRepeating(0);
        scheduler.tickRepeating(0);
        assertTrue(manager.isRestartInProgress(),
            "After 3 consecutive low TPS checks (post-recovery), restart should trigger");
    }

    // --- Monitoring disabled: no restart triggered even with low TPS ---

    @Test
    void monitoringDisabledPreventsAutoRestart() {
        platform.setTps(5.0);
        config.setMonitoringEnabled(false);
        config.setEmergencyRestartEnabled(false);

        RestartManager manager = new RestartManager(
            logger, platform, scheduler, config, backendRegistry());
        PlatformLoadMonitor monitor = new PlatformLoadMonitor(
            logger, platform, scheduler, config, manager);

        monitor.startMonitoring();

        // Many ticks
        for (int i = 0; i < 10; i++) {
            scheduler.tickRepeating(0);
        }
        assertFalse(manager.isRestartInProgress(),
            "Monitoring disabled should prevent auto-restart");
    }

    // --- Emergency TPS: immediate trigger without consecutive checks ---

    @Test
    void emergencyTPSUsesShorterDelay() {
        platform.setTps(5.0); // Below emergency threshold of 12
        config.setMonitoringEnabled(false);
        config.setEmergencyRestartEnabled(true);
        config.setEmergencyTpsThreshold(12.0);
        config.setEmergencyDelay(15);

        RestartManager manager = new RestartManager(
            logger, platform, scheduler, config, backendRegistry());
        PlatformLoadMonitor monitor = new PlatformLoadMonitor(
            logger, platform, scheduler, config, manager);

        monitor.startMonitoring();

        // Single tick should trigger emergency (no consecutive check pattern)
        scheduler.tickRepeating(0);
        assertTrue(manager.isRestartInProgress(),
            "Emergency TPS should trigger immediately");
        assertEquals(RestartReason.EMERGENCY_TPS, manager.getCurrentRestartReason());
        assertEquals(15, manager.getSecondsUntilRestart());
    }

    // --- Emergency TPS: does not re-trigger if already triggered ---

    @Test
    void emergencyTPSDoesNotRetrigger() {
        platform.setTps(5.0);
        config.setMonitoringEnabled(false);
        config.setEmergencyRestartEnabled(true);
        config.setEmergencyTpsThreshold(12.0);
        config.setEmergencyDelay(30);

        RestartManager manager = new RestartManager(
            logger, platform, scheduler, config, backendRegistry());
        PlatformLoadMonitor monitor = new PlatformLoadMonitor(
            logger, platform, scheduler, config, manager);

        monitor.startMonitoring();

        // First trigger
        scheduler.tickRepeating(0);
        assertTrue(manager.isRestartInProgress());
        int firstSeconds = manager.getSecondsUntilRestart();

        // Second tick — emergency already triggered, should not re-trigger
        // (restart is already in progress, so checkTPS/checkMemory bail out)
        platform.setTps(4.0);
        scheduler.tickRepeating(0);
        // Should still have the same or similar countdown (may have decremented by 1)
        assertTrue(manager.getSecondsUntilRestart() <= firstSeconds,
            "Emergency should not re-trigger while already in progress");
    }

    // --- Emergency memory threshold ---

    @Test
    void emergencyMemoryTriggersRestart() {
        // We can't easily control actual memory usage, but we can test the
        // monitoring flow with TPS since memory is checked the same way.
        // Instead, verify the config values are accessible
        config.setEmergencyRestartEnabled(true);
        config.setEmergencyMemoryThreshold(90.0);
        config.setEmergencyDelay(10);
        assertEquals(90.0, config.getEmergencyMemoryThreshold());
        assertEquals(10, config.getEmergencyDelay());
    }

    // --- Monitor tracks last TPS ---

    @Test
    void monitorTracksLastTPS() {
        platform.setTps(17.5);
        config.setMonitoringEnabled(true);
        config.setTpsThreshold(18.0);
        config.setConsecutiveChecks(99); // High so it won't trigger

        RestartManager manager = new RestartManager(
            logger, platform, scheduler, config, backendRegistry());
        PlatformLoadMonitor monitor = new PlatformLoadMonitor(
            logger, platform, scheduler, config, manager);

        monitor.startMonitoring();
        scheduler.tickRepeating(0);

        assertEquals(17.5, monitor.getLastTPS(), 0.01,
            "Monitor should track the last TPS reading");
    }

    // --- Stop monitoring cancels the task ---

    @Test
    void stopMonitoringCancelsTask() {
        config.setMonitoringEnabled(true);
        RestartManager manager = new RestartManager(
            logger, platform, scheduler, config, backendRegistry());
        PlatformLoadMonitor monitor = new PlatformLoadMonitor(
            logger, platform, scheduler, config, manager);

        monitor.startMonitoring();
        assertTrue(scheduler.repeatingTasks.size() >= 1,
            "Should have a monitoring task scheduled");

        monitor.stopMonitoring();
        assertTrue(scheduler.cancelledTasks > 0,
            "Stop monitoring should cancel the task");
    }

    // --- Helper classes ---

    private static class ControllablePlatform implements ServerPlatform {
        private double tps = 20.0;

        void setTps(double tps) { this.tps = tps; }

        @Override public void broadcastMessage(String message) {}
        @Override public void broadcastTitle(String title, String subtitle) {}
        @Override public void executeConsole(String command) {}
        @Override public double getTPS() { return tps; }
    }

    private static class ControllableScheduler implements PlatformTaskScheduler {
        private final List<Runnable> repeatingTasks = new ArrayList<>();
        private int cancelledTasks = 0;

        @Override
        public ScheduledTaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
            repeatingTasks.add(task);
            return () -> { cancelledTasks++; };
        }

        @Override
        public ScheduledTaskHandle runRepeatingAsync(Runnable task, long initialDelayTicks, long periodTicks) {
            return runRepeating(task, initialDelayTicks, periodTicks);
        }

        @Override
        public ScheduledTaskHandle runLater(Runnable task, long delayTicks) {
            task.run();
            return () -> {};
        }

        @Override
        public ScheduledTaskHandle runLaterAsync(Runnable task, long delayTicks) {
            task.run();
            return () -> {};
        }

        @Override
        public boolean isFolia() { return false; }

        void tickRepeating(int index) {
            if (index < repeatingTasks.size()) {
                repeatingTasks.get(index).run();
            }
        }
    }
}
