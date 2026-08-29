package dev.demonz.redstonereboot.common.schedule;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestartScheduleCalculatorTest {

    @Test
    void returnsNextTimeLaterTheSameDay() {
        ZonedDateTime now = ZonedDateTime.of(2026, 4, 11, 10, 15, 0, 0, ZoneId.of("Asia/Kolkata"));

        ZonedDateTime nextRestart = RestartScheduleCalculator.calculateNextRestart(
            now,
            List.of("06:00", "12:00", "18:00"),
            List.of("ALL")
        ).orElseThrow();

        assertEquals(ZonedDateTime.of(2026, 4, 11, 12, 0, 0, 0, ZoneId.of("Asia/Kolkata")), nextRestart);
    }

    @Test
    void rollsForwardToTheNextAllowedDay() {
        ZonedDateTime now = ZonedDateTime.of(2026, 4, 11, 20, 0, 0, 0, ZoneId.of("UTC"));

        ZonedDateTime nextRestart = RestartScheduleCalculator.calculateNextRestart(
            now,
            List.of("08:00"),
            List.of("MONDAY")
        ).orElseThrow();

        assertEquals(ZonedDateTime.of(2026, 4, 13, 8, 0, 0, 0, ZoneId.of("UTC")), nextRestart);
    }

    @Test
    void skipsInvalidTimesInsteadOfCrashing() {
        ZonedDateTime now = ZonedDateTime.of(2026, 4, 11, 10, 0, 0, 0, ZoneId.of("UTC"));

        ZonedDateTime nextRestart = RestartScheduleCalculator.calculateNextRestart(
            now,
            List.of("bad", "15:30"),
            List.of("ALL")
        ).orElseThrow();

        assertEquals(ZonedDateTime.of(2026, 4, 11, 15, 30, 0, 0, ZoneId.of("UTC")), nextRestart);
    }

    @Test
    void returnsEmptyWhenNoValidConfigurationExists() {
        ZonedDateTime now = ZonedDateTime.of(2026, 4, 11, 10, 0, 0, 0, ZoneId.of("UTC"));

        assertTrue(RestartScheduleCalculator.calculateNextRestart(
            now,
            List.of("bad"),
            List.of("NOT_A_DAY")
        ).isEmpty());
    }
}
