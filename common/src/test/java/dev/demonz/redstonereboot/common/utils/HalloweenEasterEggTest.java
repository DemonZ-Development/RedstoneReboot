package dev.demonz.redstonereboot.common.utils;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class HalloweenEasterEggTest {
    @Test
    void startupCountdownAndHolidayAreLimitedToOctober() {
        String[][] cases = {
            {"2026-09-30T12:00:00Z", ""},
            {"2026-10-01T12:00:00Z", "Halloween in 30 days!"},
            {"2026-10-02T12:00:00Z", "Halloween in 29 days!"},
            {"2026-10-30T12:00:00Z", "Halloween in 1 day!"},
            {"2026-10-31T12:00:00Z", "Happy Halloween!"},
            {"2026-11-01T12:00:00Z", ""},
            {"2028-10-31T12:00:00Z", "Happy Halloween!"}
        };
        for (String[] example : cases) {
            try (Fixture f = new Fixture(example[0], "UTC")) {
                f.egg.start();
                assertEquals(example[1].isEmpty() ? List.of() : List.of(example[1]), f.console);
            }
        }
    }

    @Test
    void identicalInstantUsesEachServersLocalCalendar() {
        try (Fixture india = new Fixture("2026-10-30T20:00:00Z", "Asia/Kolkata");
             Fixture california = new Fixture("2026-10-30T20:00:00Z", "America/Los_Angeles")) {
            india.egg.start();
            california.egg.start();
            assertEquals(List.of("Happy Halloween!"), india.console);
            assertEquals(List.of("Halloween in 1 day!"), california.console);
        }
    }

    @Test
    void quietChecksRepeatEverySevenWallClockHours() {
        try (Fixture f = new Fixture("2026-10-01T12:00:00Z", "UTC")) {
            f.egg.start();
            assertEquals(TimeUnit.HOURS.toMillis(7), f.scheduler.periodMillis);
            assertEquals(TimeUnit.HOURS.toMillis(7), f.scheduler.initialDelayMillis);
            f.clock.instant = Instant.parse("2026-10-02T12:00:00Z");
            f.scheduler.periodic.run();
            assertEquals(List.of("Halloween in 30 days!"), f.console);
            assertEquals(0, f.notifications.get());
        }
    }

    @Test
    void localMidnightGreetsWithoutWaitingForTheNextPeriodicCheck() {
        try (Fixture f = new Fixture("2026-10-30T18:29:30Z", "Asia/Kolkata")) {
            f.egg.start();
            assertEquals(30_000, f.scheduler.midnightDelayMillis);
            f.clock.instant = Instant.parse("2026-10-30T18:30:00Z");
            f.scheduler.midnight.run();
            f.scheduler.periodic.run();
            assertEquals(List.of("Halloween in 1 day!", "Happy Halloween!"), f.console);
            assertEquals(2, f.notifications.get());
        }
    }

    @Test
    void daylightSavingChangeIsIncludedInTheMidnightDelay() {
        try (Fixture f = new Fixture("2026-10-24T22:00:00Z", "Europe/Berlin")) {
            f.egg.start();
            assertEquals(TimeUnit.HOURS.toMillis(145), f.scheduler.midnightDelayMillis);
        }
    }

    @Test
    void lateJoiningPlayersAreGreetedOncePerYear() {
        try (Fixture f = new Fixture("2026-10-30T12:00:00Z", "UTC")) {
            UUID player = UUID.randomUUID();
            List<String> messages = new ArrayList<>();
            f.egg.start();
            f.egg.greetPlayer(player, messages::add);
            assertTrue(messages.isEmpty());
            f.clock.instant = Instant.parse("2026-10-31T12:00:00Z");
            f.scheduler.periodic.run();
            f.egg.greetPlayer(player, messages::add);
            f.egg.greetPlayer(player, messages::add);
            assertEquals(List.of("Happy Halloween!"), messages);
            f.clock.instant = Instant.parse("2027-10-31T12:00:00Z");
            f.scheduler.periodic.run();
            f.egg.greetPlayer(player, messages::add);
            assertEquals(List.of("Happy Halloween!", "Happy Halloween!"), messages);
        }
    }

    @Test
    void closingStopsBackgroundChecksAndQueuedPlayerGreetings() {
        try (Fixture f = new Fixture("2026-10-30T12:00:00Z", "UTC")) {
            f.egg.start();
            f.egg.close();
            f.clock.instant = Instant.parse("2026-10-31T12:00:00Z");
            f.scheduler.periodic.run();
            f.egg.greetPlayer(UUID.randomUUID(), ignored -> fail("Greeting after shutdown"));
            assertTrue(f.scheduler.isShutdown());
            assertEquals(List.of("Halloween in 1 day!"), f.console);
            assertEquals(0, f.notifications.get());
        }
    }

    private static final class Fixture implements AutoCloseable {
        final MutableClock clock;
        final RecordingScheduler scheduler = new RecordingScheduler();
        final List<String> console = new ArrayList<>();
        final AtomicInteger notifications = new AtomicInteger();
        final HalloweenEasterEgg egg;

        Fixture(String instant, String zone) {
            clock = new MutableClock(Instant.parse(instant), ZoneId.of(zone));
            egg = new HalloweenEasterEgg(clock, scheduler, console::add, notifications::incrementAndGet);
        }

        @Override public void close() { egg.close(); }
    }

    private static final class MutableClock extends Clock {
        Instant instant;
        final ZoneId zone;
        MutableClock(Instant instant, ZoneId zone) { this.instant = instant; this.zone = zone; }
        @Override public Instant instant() { return instant; }
        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant, zone); }
    }

    private static final class RecordingScheduler extends ScheduledThreadPoolExecutor {
        Runnable periodic;
        Runnable midnight;
        long initialDelayMillis;
        long periodMillis;
        long midnightDelayMillis;

        RecordingScheduler() { super(1); }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initial, long delay, TimeUnit unit) {
            periodic = task;
            initialDelayMillis = unit.toMillis(initial);
            periodMillis = unit.toMillis(delay);
            return super.scheduleWithFixedDelay(task, 365, 365, TimeUnit.DAYS);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
            midnight = task;
            midnightDelayMillis = unit.toMillis(delay);
            return super.schedule(task, 365, TimeUnit.DAYS);
        }
    }
}
