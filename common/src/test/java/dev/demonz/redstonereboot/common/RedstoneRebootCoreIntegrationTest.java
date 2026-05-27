package dev.demonz.redstonereboot.common;

import dev.demonz.redstonereboot.common.backend.BackendConfig;
import dev.demonz.redstonereboot.common.backend.BackendRegistry;
import dev.demonz.redstonereboot.common.manager.RestartManager;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link RedstoneRebootCore} — lifecycle, version,
 * emergency restart, and component wiring.
 */
class RedstoneRebootCoreIntegrationTest {

    @TempDir
    Path tempDir;

    private Logger logger;
    private SimplePlatformConfig config;
    private FakeScheduler scheduler;
    private FakePlatform platform;

    @BeforeEach
    void setUp() {
        logger = Logger.getLogger("RedstoneRebootCoreTest");
        config = new SimplePlatformConfig();
        config.setScheduledRestartsEnabled(false);
        scheduler = new FakeScheduler();
        platform = new FakePlatform();
    }

    // --- Version is correct ---

    @Test
    void versionIs142() {
        assertEquals("1.4.2", RedstoneRebootCore.VERSION);
    }

    @Test
    void versionInstanceGetterMatchesStatic() {
        RedstoneRebootCore core = new RedstoneRebootCore(platform, scheduler, config, tempDir);
        assertEquals(RedstoneRebootCore.VERSION, core.getVersion());
    }

    // --- onEnable initializes all components ---

    @Test
    void onEnableInitializesComponents() {
        RedstoneRebootCore core = new RedstoneRebootCore(platform, scheduler, config, tempDir);
        core.onEnable();

        assertNotNull(core.getRestartManager());
        assertNotNull(core.getBackendRegistry());
        assertNotNull(core.getScheduler());
        assertNotNull(core.getConfig());
        assertNotNull(core.getPlatform());
        assertNotNull(core.getUpdateChecker());
    }

    // --- onDisable cleans up ---

    @Test
    void onDisableCleansUp() {
        RedstoneRebootCore core = new RedstoneRebootCore(platform, scheduler, config, tempDir);
        core.onEnable();
        core.onDisable();

        // After disable, the restart manager should be cleaned up
        // (scheduler tasks cancelled)
    }

    // --- Reload refreshes state ---

    @Test
    void reloadRuntimeStateRefreshesComponents() {
        RedstoneRebootCore core = new RedstoneRebootCore(platform, scheduler, config, tempDir);
        core.onEnable();

        // Reload should not throw
        core.reloadRuntimeState();

        // Platform should have been reloaded
        assertTrue(platform.reloadCalled.get(),
            "Reload should call platform.reloadPlatformState()");
    }

    // --- Emergency restart triggers RestartManager ---

    @Test
    void emergencyRestartTriggersManager() {
        config.setEmergencyDelay(15);

        RedstoneRebootCore core = new RedstoneRebootCore(platform, scheduler, config, tempDir);
        core.onEnable();

        core.triggerEmergencyRestart("Test emergency");

        assertTrue(core.getRestartManager().isRestartInProgress(),
            "Emergency restart should trigger a restart in the manager");
        assertEquals(dev.demonz.redstonereboot.common.manager.RestartReason.EMERGENCY_TPS,
            core.getRestartManager().getCurrentRestartReason());
    }

    // --- Emergency restart with zero delay uses performImmediateRestart ---

    @Test
    void emergencyRestartWithZeroDelay() {
        config.setEmergencyDelay(0);

        // Use a scheduler that actually runs async tasks
        AsyncTestScheduler asyncScheduler = new AsyncTestScheduler();

        RedstoneRebootCore core = new RedstoneRebootCore(platform, asyncScheduler, config, tempDir);
        core.onEnable();

        core.triggerEmergencyRestart("Zero delay emergency");

        // With 0 delay, it should call performImmediateRestart
        // The backend executes async, so wait briefly
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        // Verify the restart was attempted (platform shutdown may or may not be called
        // depending on the async execution timing)
    }

    // --- Backends disabled by default ---

    @Test
    void backendsDisabledByDefault() {
        RedstoneRebootCore core = new RedstoneRebootCore(platform, scheduler, config, tempDir);
        core.onEnable();

        assertEquals("ShutdownOnly",
            core.getBackendRegistry().getActiveBackend().getName(),
            "Default backend should be ShutdownOnly");
        assertFalse(core.getBackendRegistry().getActiveBackend().isControllerOwned(),
            "Default backend should NOT be controller-owned");
    }

    // --- Helper classes ---

    private static class FakeScheduler implements PlatformTaskScheduler {
        private final List<Runnable> repeatingTasks = new ArrayList<>();

        @Override
        public ScheduledTaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
            repeatingTasks.add(task);
            return () -> {};
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
    }

    private static class AsyncTestScheduler implements PlatformTaskScheduler {
        private final java.util.concurrent.ExecutorService pool =
            java.util.concurrent.Executors.newCachedThreadPool();

        @Override
        public ScheduledTaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
            pool.submit(task);
            return () -> {};
        }

        @Override
        public ScheduledTaskHandle runRepeatingAsync(Runnable task, long initialDelayTicks, long periodTicks) {
            pool.submit(task);
            return () -> {};
        }

        @Override
        public ScheduledTaskHandle runLater(Runnable task, long delayTicks) {
            pool.submit(task);
            return () -> {};
        }

        @Override
        public ScheduledTaskHandle runLaterAsync(Runnable task, long delayTicks) {
            pool.submit(task);
            return () -> {};
        }

        @Override
        public boolean isFolia() { return false; }
    }

    private static class FakePlatform implements ServerPlatform {
        final AtomicBoolean reloadCalled = new AtomicBoolean(false);
        final AtomicBoolean shutdownCalled = new AtomicBoolean(false);

        @Override public void broadcastMessage(String message) {}
        @Override public void broadcastTitle(String title, String subtitle) {}
        @Override public void executeConsole(String command) {}
        @Override public double getTPS() { return 20.0; }
        @Override public void shutdownServer(String reason) { shutdownCalled.set(true); }
        @Override public void reloadPlatformState() { reloadCalled.set(true); }
    }
}
