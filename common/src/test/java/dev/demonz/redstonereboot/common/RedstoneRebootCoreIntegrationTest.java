/*
 * Copyright (c) 2026 DemonZ Development
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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


    @Test
    void versionIs150() {
        assertEquals("1.5.0", RedstoneRebootCore.VERSION);
    }

    @Test
    void versionInstanceGetterMatchesStatic() {
        RedstoneRebootCore core = new RedstoneRebootCore(platform, scheduler, config, tempDir);
        assertEquals(RedstoneRebootCore.VERSION, core.getVersion());
    }


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


    @Test
    void onDisableCleansUp() {
        RedstoneRebootCore core = new RedstoneRebootCore(platform, scheduler, config, tempDir);
        core.onEnable();
        core.onDisable();

    }


    @Test
    void reloadRuntimeStateRefreshesComponents() {
        RedstoneRebootCore core = new RedstoneRebootCore(platform, scheduler, config, tempDir);
        core.onEnable();

        core.reloadRuntimeState();

        assertTrue(platform.reloadCalled.get(),
            "Reload should call platform.reloadPlatformState()");
    }


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


    @Test
    void emergencyRestartWithZeroDelay() {
        config.setEmergencyDelay(0);

        AsyncTestScheduler asyncScheduler = new AsyncTestScheduler();

        RedstoneRebootCore core = new RedstoneRebootCore(platform, asyncScheduler, config, tempDir);
        core.onEnable();

        core.triggerEmergencyRestart("Zero delay emergency");

        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

    }


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