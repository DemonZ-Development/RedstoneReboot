package dev.demonz.redstonereboot.common.monitor;

import dev.demonz.redstonereboot.common.backend.BackendConfig;
import dev.demonz.redstonereboot.common.backend.BackendRegistry;
import dev.demonz.redstonereboot.common.manager.RestartManager;
import dev.demonz.redstonereboot.common.manager.RestartReason;
import dev.demonz.redstonereboot.common.platform.ServerPlatform;
import dev.demonz.redstonereboot.common.platform.SimplePlatformConfig;
import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import dev.demonz.redstonereboot.common.scheduler.ScheduledTaskHandle;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformLoadMonitorTest {

    @Test
    void emergencyCheckCanShortenExistingCountdown() {
        FakePlatform platform = new FakePlatform();
        platform.setTps(5.0);

        SimplePlatformConfig config = new SimplePlatformConfig();
        config.setMonitoringEnabled(false);
        config.setEmergencyRestartEnabled(true);
        config.setEmergencyDelay(30);
        config.setEmergencyTpsThreshold(12.0);

        FakeScheduler scheduler = new FakeScheduler();
        RestartManager manager = new RestartManager(
            Logger.getLogger("PlatformLoadMonitorTest"),
            platform,
            scheduler,
            config,
            backendRegistry()
        );

        manager.scheduleRestart(120, RestartReason.MANUAL, "tester");

        PlatformLoadMonitor monitor = new PlatformLoadMonitor(
            Logger.getLogger("PlatformLoadMonitorTest"),
            platform,
            scheduler,
            config,
            manager
        );

        monitor.startMonitoring();
        scheduler.runRepeatingTask(1);

        assertTrue(manager.isRestartInProgress());
        assertEquals(RestartReason.EMERGENCY_TPS, manager.getCurrentRestartReason());
        assertEquals(30, manager.getSecondsUntilRestart());
    }

    private static BackendRegistry backendRegistry() {
        Logger logger = Logger.getLogger("PlatformLoadMonitorTest");
        return new BackendRegistry(logger, new BackendConfig(Path.of("build", "tmp", "PlatformLoadMonitorTest"), logger));
    }

    private static final class FakeScheduler implements PlatformTaskScheduler {
        private final List<Runnable> repeatingTasks = new ArrayList<>();

        @Override
        public ScheduledTaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
            repeatingTasks.add(task);
            return () -> { };
        }

        @Override
        public ScheduledTaskHandle runLater(Runnable task, long delayTicks) {
            return () -> { };
        }

        @Override
        public boolean isFolia() {
            return false;
        }

        void runRepeatingTask(int index) {
            repeatingTasks.get(index).run();
        }
    }

    private static final class FakePlatform implements ServerPlatform {
        private double tps = 20.0;

        @Override
        public void broadcastMessage(String message) {
        }

        @Override
        public void broadcastTitle(String title, String subtitle) {
        }

        @Override
        public void executeConsole(String command) {
        }

        @Override
        public double getTPS() {
            return tps;
        }

        void setTps(double tps) {
            this.tps = tps;
        }
    }
}
