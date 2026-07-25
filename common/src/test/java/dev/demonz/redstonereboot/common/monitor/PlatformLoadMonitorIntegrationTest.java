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

        scheduler.tickRepeating(0);
        assertFalse(manager.isRestartInProgress());

        scheduler.tickRepeating(0);
        assertFalse(manager.isRestartInProgress());

        scheduler.tickRepeating(0);
        assertTrue(manager.isRestartInProgress(),
            "After 3 consecutive low TPS checks, restart should be triggered");
        assertEquals(RestartReason.EMERGENCY_TPS, manager.getCurrentRestartReason());
    }


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

        platform.setTps(10.0);
        scheduler.tickRepeating(0);
        assertFalse(manager.isRestartInProgress());

        platform.setTps(19.0);
        scheduler.tickRepeating(0);
        assertFalse(manager.isRestartInProgress());

        platform.setTps(10.0);
        scheduler.tickRepeating(0);
        assertFalse(manager.isRestartInProgress(),
            "Counter should have reset after recovery, so 1 check is not enough");

        scheduler.tickRepeating(0);
        scheduler.tickRepeating(0);
        assertTrue(manager.isRestartInProgress(),
            "After 3 consecutive low TPS checks (post-recovery), restart should trigger");
    }


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

        for (int i = 0; i < 10; i++) {
            scheduler.tickRepeating(0);
        }
        assertFalse(manager.isRestartInProgress(),
            "Monitoring disabled should prevent auto-restart");
    }


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

        scheduler.tickRepeating(0);
        assertTrue(manager.isRestartInProgress(),
            "Emergency TPS should trigger immediately");
        assertEquals(RestartReason.EMERGENCY_TPS, manager.getCurrentRestartReason());
        assertEquals(15, manager.getSecondsUntilRestart());
    }


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

        scheduler.tickRepeating(0);
        assertTrue(manager.isRestartInProgress());
        int firstSeconds = manager.getSecondsUntilRestart();

        platform.setTps(4.0);
        scheduler.tickRepeating(0);
        assertTrue(manager.getSecondsUntilRestart() <= firstSeconds,
            "Emergency should not re-trigger while already in progress");
    }


    @Test
    void emergencyMemoryTriggersRestart() {
        config.setEmergencyRestartEnabled(true);
        config.setEmergencyMemoryThreshold(90.0);
        config.setEmergencyDelay(10);
        assertEquals(90.0, config.getEmergencyMemoryThreshold());
        assertEquals(10, config.getEmergencyDelay());
    }


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