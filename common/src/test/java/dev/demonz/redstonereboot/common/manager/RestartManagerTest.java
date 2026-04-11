package dev.demonz.redstonereboot.common.manager;

import dev.demonz.redstonereboot.common.platform.ServerPlatform;
import dev.demonz.redstonereboot.common.platform.SimplePlatformConfig;
import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import dev.demonz.redstonereboot.common.scheduler.ScheduledTaskHandle;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestartManagerTest {

    @Test
    void clampsScheduledCountdownWhenServerStartsInsideWarningWindow() {
        ZonedDateTime now = ZonedDateTime.of(2026, 4, 11, 17, 58, 0, 0, ZoneId.of("Asia/Kolkata"));
        SimplePlatformConfig config = new SimplePlatformConfig();
        config.setScheduledRestartsEnabled(true);
        config.setScheduledTimes(List.of("18:00"));
        config.setScheduledDays(List.of("ALL"));
        config.setTimezone("Asia/Kolkata");
        config.setScheduledWarningTime(300);

        FakeScheduler scheduler = new FakeScheduler();
        RestartManager manager = new RestartManager(
            Logger.getLogger("RestartManagerTest"),
            new FakePlatform(),
            scheduler,
            config,
            () -> now
        );

        manager.scheduleRestarts();
        scheduler.runRepeatingTask(0);

        assertTrue(manager.isRestartInProgress());
        assertEquals(120, manager.getSecondsUntilRestart());
    }

    @Test
    void cancelRestartResetsReasonToUnknown() {
        FakeScheduler scheduler = new FakeScheduler();
        RestartManager manager = new RestartManager(
            Logger.getLogger("RestartManagerTest"),
            new FakePlatform(),
            scheduler,
            new SimplePlatformConfig(),
            () -> ZonedDateTime.now(ZoneId.of("UTC"))
        );

        manager.scheduleRestart(30, RestartReason.MANUAL, "tester");
        manager.cancelRestart();

        assertEquals(RestartReason.UNKNOWN, manager.getCurrentRestartReason());
        assertEquals(-1, manager.getSecondsUntilRestart());
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
            return 20.0;
        }
    }
}
