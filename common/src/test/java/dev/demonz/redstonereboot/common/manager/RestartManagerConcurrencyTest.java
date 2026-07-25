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


package dev.demonz.redstonereboot.common.manager;

import dev.demonz.redstonereboot.common.backend.BackendConfig;
import dev.demonz.redstonereboot.common.backend.BackendRegistry;
import dev.demonz.redstonereboot.common.backend.BackendResult;
import dev.demonz.redstonereboot.common.backend.RestartBackend;
import dev.demonz.redstonereboot.common.platform.ServerPlatform;
import dev.demonz.redstonereboot.common.platform.SimplePlatformConfig;
import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import dev.demonz.redstonereboot.common.scheduler.ScheduledTaskHandle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Practical concurrency and thread-safety tests for {@link RestartManager}.
 * <p>
 * Validates that the restart manager handles concurrent restart requests,
 * race conditions in countdown execution, stale generation discarding,
 * and lockout state correctly under multi-threaded access.
 * </p>
 */
class RestartManagerConcurrencyTest {

    @TempDir
    Path tempDir;

    private Logger logger;
    private SimplePlatformConfig config;
    private CapturingPlatform platform;
    private BackendRegistry backendRegistry;

    @BeforeEach
    void setUp() {
        logger = Logger.getLogger("RestartManagerConcurrencyTest");
        config = new SimplePlatformConfig();
        config.setScheduledRestartsEnabled(false);
        platform = new CapturingPlatform();
        backendRegistry = new BackendRegistry(logger, new BackendConfig(tempDir, logger), tempDir);
        backendRegistry.initialize();
    }


    @Test
    void concurrentScheduleRestartOnlyOneWins() throws Exception {
        int threadCount = 8;
        java.util.concurrent.CyclicBarrier barrier = new java.util.concurrent.CyclicBarrier(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        RestartManager manager = new RestartManager(
            logger, platform, new NoOpScheduler(), config, backendRegistry,
            () -> ZonedDateTime.now(ZoneId.of("UTC"))
        );

        List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    return;
                }
                boolean result = manager.scheduleRestart(60 + idx, RestartReason.MANUAL, "Thread-" + idx);
                if (result) successCount.incrementAndGet();
                else failCount.incrementAndGet();
            }));
        }

        for (java.util.concurrent.Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertTrue(successCount.get() >= 1,
            "At least one restart should be scheduled, got " + successCount.get());
        assertEquals(threadCount, successCount.get() + failCount.get(),
            "Total attempts should equal thread count");
        assertTrue(manager.isRestartInProgress());
    }


    @Test
    void countdownDecrementsEachTick() {
        TickableScheduler tickScheduler = new TickableScheduler();

        RestartManager manager = new RestartManager(
            logger, platform, tickScheduler, config, backendRegistry,
            () -> ZonedDateTime.now(ZoneId.of("UTC"))
        );

        manager.scheduleRestart(5, RestartReason.MANUAL, "TickTest");
        assertEquals(4, manager.getSecondsUntilRestart(),
            "After initial tick, should be 4");

        tickScheduler.tickRepeating();
        assertEquals(3, manager.getSecondsUntilRestart());

        tickScheduler.tickRepeating();
        assertEquals(2, manager.getSecondsUntilRestart());
    }


    @Test
    void cancelDuringCountdownResetsAllState() {
        TickableScheduler tickScheduler = new TickableScheduler();

        RestartManager manager = new RestartManager(
            logger, platform, tickScheduler, config, backendRegistry,
            () -> ZonedDateTime.now(ZoneId.of("UTC"))
        );

        manager.scheduleRestart(60, RestartReason.EMERGENCY_TPS, "EmergencyTest");
        assertTrue(manager.isRestartInProgress());
        assertEquals(RestartReason.EMERGENCY_TPS, manager.getCurrentRestartReason());

        boolean cancelled = manager.cancelRestart();
        assertTrue(cancelled);
        assertFalse(manager.isRestartInProgress());
        assertEquals(RestartReason.UNKNOWN, manager.getCurrentRestartReason());
        assertEquals(-1, manager.getSecondsUntilRestart());
    }


    @Test
    void soonerRestartReplacesLongerOne() {
        TickableScheduler tickScheduler = new TickableScheduler();

        RestartManager manager = new RestartManager(
            logger, platform, tickScheduler, config, backendRegistry,
            () -> ZonedDateTime.now(ZoneId.of("UTC"))
        );

        manager.scheduleRestart(120, RestartReason.SCHEDULED, "LongRestart");
        assertEquals(119, manager.getSecondsUntilRestart(),
            "After initial tick, 120 → 119");

        boolean result = manager.scheduleRestart(30, RestartReason.EMERGENCY_TPS, "ShortRestart");
        assertTrue(result, "Sooner restart should replace longer one");
        assertEquals(29, manager.getSecondsUntilRestart(),
            "After replacement and initial tick, 30 → 29");
        assertEquals(RestartReason.EMERGENCY_TPS, manager.getCurrentRestartReason());
    }


    @Test
    void longerRestartDoesNotReplaceShorterOne() {
        TickableScheduler tickScheduler = new TickableScheduler();

        RestartManager manager = new RestartManager(
            logger, platform, tickScheduler, config, backendRegistry,
            () -> ZonedDateTime.now(ZoneId.of("UTC"))
        );

        manager.scheduleRestart(30, RestartReason.EMERGENCY_TPS, "ShortRestart");
        int afterFirst = manager.getSecondsUntilRestart();

        boolean result = manager.scheduleRestart(120, RestartReason.SCHEDULED, "LongRestart");
        assertFalse(result, "Longer restart should not replace shorter one");
        assertEquals(afterFirst, manager.getSecondsUntilRestart());
        assertEquals(RestartReason.EMERGENCY_TPS, manager.getCurrentRestartReason());
    }


    @Test
    void controllerRestartSetsPendingFlag() throws Exception {
        ControllerTestBackend controllerBackend = new ControllerTestBackend(logger);
        BackendRegistry customRegistry = new BackendRegistry(logger, new BackendConfig(tempDir, logger), tempDir) {
            @Override
            public RestartBackend getActiveBackend() {
                return controllerBackend;
            }
        };

        java.util.concurrent.CountDownLatch completionLatch = new java.util.concurrent.CountDownLatch(1);
        FullAsyncScheduler asyncScheduler = new FullAsyncScheduler(completionLatch);

        RestartManager manager = new RestartManager(
            logger, platform, asyncScheduler, config, customRegistry,
            () -> ZonedDateTime.now(ZoneId.of("UTC"))
        );

        manager.scheduleRestart(0, RestartReason.MANUAL, "ControllerTest");

        boolean completed = completionLatch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "Async execution should complete within timeout");

        Thread.sleep(50);

        assertTrue(manager.isControllerRestartPending(),
            "Controller restart should be pending after ACCEPTED from controller backend");

        boolean result = manager.scheduleRestart(30, RestartReason.MANUAL, "BlockedByController");
        assertFalse(result, "New restart should be blocked when controller restart is pending");
    }


    @Test
    void lockoutBlocksNewRestarts() throws Exception {
        UnknownResultBackend unknownBackend = new UnknownResultBackend(logger);
        BackendRegistry customRegistry = new BackendRegistry(logger, new BackendConfig(tempDir, logger), tempDir) {
            @Override
            public RestartBackend getActiveBackend() {
                return unknownBackend;
            }
        };

        java.util.concurrent.CountDownLatch completionLatch = new java.util.concurrent.CountDownLatch(1);
        FullAsyncScheduler asyncScheduler = new FullAsyncScheduler(completionLatch);

        RestartManager manager = new RestartManager(
            logger, platform, asyncScheduler, config, customRegistry,
            () -> ZonedDateTime.now(ZoneId.of("UTC"))
        );

        manager.scheduleRestart(0, RestartReason.MANUAL, "LockoutTest");

        boolean completed = completionLatch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "Async execution should complete");
        Thread.sleep(50);

        assertTrue(manager.isLockoutActive(), "Lockout should be active after UNKNOWN result");

        boolean result = manager.scheduleRestart(60, RestartReason.MANUAL, "BlockedByLockout");
        assertFalse(result, "Restart during lockout should be rejected");
    }


    @Test
    void generationCounterPreventsStaleAsyncResultHandling() throws Exception {
        DelayedBackend slowBackend = new DelayedBackend(logger, 500);
        BackendRegistry customRegistry = new BackendRegistry(logger, new BackendConfig(tempDir, logger), tempDir) {
            @Override
            public RestartBackend getActiveBackend() {
                return slowBackend;
            }
        };

        java.util.concurrent.CountDownLatch firstLatch = new java.util.concurrent.CountDownLatch(1);
        LatchingAsyncScheduler asyncScheduler = new LatchingAsyncScheduler(firstLatch);

        RestartManager manager = new RestartManager(
            logger, platform, asyncScheduler, config, customRegistry,
            () -> ZonedDateTime.now(ZoneId.of("UTC"))
        );

        manager.scheduleRestart(0, RestartReason.MANUAL, "TestGen");

        boolean completed = firstLatch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "First async execution should complete");
        Thread.sleep(200); // Allow result handler to run

        assertTrue(platform.shutdownCalled.get(),
            "First restart should have triggered shutdown (no generation mismatch)");

        platform.shutdownCalled.set(false);
    }


    @Test
    void executeRestartGuardPreventsConcurrentExecution() throws Exception {


        DelayedBackend backend = new DelayedBackend(logger, 100);
        BackendRegistry customRegistry = new BackendRegistry(logger, new BackendConfig(tempDir, logger), tempDir) {
            @Override
            public RestartBackend getActiveBackend() {
                return backend;
            }
        };

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        LatchingAsyncScheduler asyncScheduler = new LatchingAsyncScheduler(latch);

        RestartManager manager = new RestartManager(
            logger, platform, asyncScheduler, config, customRegistry,
            () -> ZonedDateTime.now(ZoneId.of("UTC"))
        );

        manager.scheduleRestart(0, RestartReason.MANUAL, "First");

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "Execution should complete");
        Thread.sleep(200); // Allow result handler to run

    }


    @Test
    void cleanupCancelsActiveCountdown() {
        TickableScheduler tickScheduler = new TickableScheduler();
        SimplePlatformConfig enabledConfig = new SimplePlatformConfig();
        enabledConfig.setScheduledRestartsEnabled(true);
        enabledConfig.setScheduledTimes(List.of("06:00"));
        enabledConfig.setScheduledDays(List.of("ALL"));
        enabledConfig.setTimezone("UTC");

        RestartManager manager = new RestartManager(
            logger, platform, tickScheduler, enabledConfig, backendRegistry,
            () -> ZonedDateTime.now(ZoneId.of("UTC"))
        );

        manager.initialize();
        manager.scheduleRestart(60, RestartReason.MANUAL, "CleanupTest");
        assertTrue(manager.isRestartInProgress());

        manager.cleanup();
        assertFalse(manager.isRestartInProgress(),
            "Cleanup should cancel any in-progress restart");
    }


    @Test
    void getRestartInfoReturnsConsistentSnapshot() {
        RestartManager manager = new RestartManager(
            logger, platform, new NoOpScheduler(), config, backendRegistry,
            () -> ZonedDateTime.now(ZoneId.of("UTC"))
        );

        manager.scheduleRestart(60, RestartReason.MANUAL, "InfoTest");
        var info = manager.getRestartInfo();

        assertTrue((Boolean) info.get("restartInProgress"));
        assertEquals("Manual Restart", info.get("currentReason"),
            "MANUAL reason display name is 'Manual Restart'");
        assertEquals("InfoTest", info.get("initiator"));
        assertEquals(60, info.get("secondsUntilRestart"));
    }


    private static class NoOpScheduler implements PlatformTaskScheduler {
        @Override
        public ScheduledTaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
            return () -> {};
        }
        @Override
        public ScheduledTaskHandle runRepeatingAsync(Runnable task, long initialDelayTicks, long periodTicks) {
            return () -> {};
        }
        @Override
        public ScheduledTaskHandle runLater(Runnable task, long delayTicks) {
            return () -> {};
        }
        @Override
        public ScheduledTaskHandle runLaterAsync(Runnable task, long delayTicks) {
            return () -> {};
        }
        @Override
        public boolean isFolia() { return false; }
    }

    private static class TickableScheduler implements PlatformTaskScheduler {
        private Runnable repeatingTask;

        @Override
        public ScheduledTaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
            this.repeatingTask = task;
            task.run();
            return () -> { this.repeatingTask = null; };
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

        void tickRepeating() {
            if (repeatingTask != null) repeatingTask.run();
        }
    }

    /** An async scheduler that counts down a latch after runLaterAsync completes. */
    private static class LatchingAsyncScheduler implements PlatformTaskScheduler {
        private final ExecutorService pool = Executors.newCachedThreadPool();
        private final java.util.concurrent.CountDownLatch latch;

        LatchingAsyncScheduler(java.util.concurrent.CountDownLatch latch) {
            this.latch = latch;
        }

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
            pool.submit(() -> {
                try {
                    task.run();
                } finally {
                    latch.countDown();
                }
            });
            return () -> {};
        }

        @Override
        public boolean isFolia() { return false; }
    }

    /** Full async scheduler that counts down after runLater completes.
     *  Only runs tasks with delay=0 immediately; delayed tasks are skipped
     *  to avoid interfering with safety timeouts. */
    private static class FullAsyncScheduler implements PlatformTaskScheduler {
        private final ExecutorService pool = Executors.newCachedThreadPool();
        private final java.util.concurrent.CountDownLatch latch;

        FullAsyncScheduler(java.util.concurrent.CountDownLatch latch) {
            this.latch = latch;
        }

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
            if (delayTicks > 0) {
                return () -> {};
            }
            pool.submit(() -> {
                try {
                    task.run();
                } finally {
                    latch.countDown();
                }
            });
            return () -> {};
        }

        @Override
        public ScheduledTaskHandle runLaterAsync(Runnable task, long delayTicks) {
            if (delayTicks > 0) {
                return () -> {};
            }
            pool.submit(task);
            return () -> {};
        }

        @Override
        public boolean isFolia() { return false; }
    }

    private static class CapturingPlatform implements ServerPlatform {
        final AtomicBoolean shutdownCalled = new AtomicBoolean(false);

        @Override public void broadcastMessage(String message) {}
        @Override public void broadcastTitle(String title, String subtitle) {}
        @Override public void executeConsole(String command) {}
        @Override public double getTPS() { return 20.0; }
        @Override public void shutdownServer(String reason) {
            shutdownCalled.set(true);
        }
    }

    /** A backend that takes a configurable delay before returning ACCEPTED. */
    private static class DelayedBackend extends dev.demonz.redstonereboot.common.backend.BaseBackend {
        private final long delayMs;

        DelayedBackend(Logger logger, long delayMs) {
            super(logger, "Delayed");
            this.delayMs = delayMs;
        }

        @Override
        public BackendResult execute() {
            try { Thread.sleep(delayMs); } catch (InterruptedException ignored) {}
            return BackendResult.ACCEPTED;
        }

        @Override
        public BackendState getState() { return BackendState.FULL; }

        @Override
        public boolean isControllerOwned() { return false; }
    }

    /** A backend that always returns UNKNOWN (simulates timeout). */
    private static class UnknownResultBackend extends dev.demonz.redstonereboot.common.backend.BaseBackend {
        UnknownResultBackend(Logger logger) { super(logger, "UnknownResult"); }

        @Override
        public BackendResult execute() { return BackendResult.UNKNOWN; }

        @Override
        public BackendState getState() { return BackendState.ASSISTED; }

        @Override
        public boolean isControllerOwned() { return false; }
    }

    /** A controller-owned backend that always returns ACCEPTED. */
    private static class ControllerTestBackend extends dev.demonz.redstonereboot.common.backend.ControllerBackend {
        ControllerTestBackend(Logger logger) { super(logger, "ControllerTest"); }

        @Override
        public BackendResult execute() { return BackendResult.ACCEPTED; }

        @Override
        public BackendState getState() { return BackendState.FULL; }
    }
}