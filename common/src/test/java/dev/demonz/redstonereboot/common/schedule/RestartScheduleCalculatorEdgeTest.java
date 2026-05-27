package dev.demonz.redstonereboot.common.schedule;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Extended edge-case tests for {@link RestartScheduleCalculator}.
 */
class RestartScheduleCalculatorEdgeTest {

    // --- Midnight restart time ---

    @Test
    void midnightRestartTime() {
        ZonedDateTime now = ZonedDateTime.of(2026, 5, 25, 23, 30, 0, 0, ZoneId.of("UTC"));

        ZonedDateTime next = RestartScheduleCalculator.calculateNextRestart(
            now,
            List.of("00:00"),
            List.of("ALL")
        ).orElseThrow();

        assertEquals(ZonedDateTime.of(2026, 5, 26, 0, 0, 0, 0, ZoneId.of("UTC")), next);
    }

    // --- Multiple times on the same day picks the nearest future one ---

    @Test
    void multipleTimesPicksNearestFuture() {
        ZonedDateTime now = ZonedDateTime.of(2026, 5, 25, 14, 0, 0, 0, ZoneId.of("UTC"));

        ZonedDateTime next = RestartScheduleCalculator.calculateNextRestart(
            now,
            List.of("06:00", "12:00", "18:00"),
            List.of("ALL")
        ).orElseThrow();

        assertEquals(ZonedDateTime.of(2026, 5, 25, 18, 0, 0, 0, ZoneId.of("UTC")), next);
    }

    // --- All times passed today rolls to tomorrow ---

    @Test
    void allTimesPassedTodayRollsToTomorrow() {
        ZonedDateTime now = ZonedDateTime.of(2026, 5, 25, 20, 0, 0, 0, ZoneId.of("UTC"));

        ZonedDateTime next = RestartScheduleCalculator.calculateNextRestart(
            now,
            List.of("06:00", "12:00"),
            List.of("ALL")
        ).orElseThrow();

        assertEquals(ZonedDateTime.of(2026, 5, 26, 6, 0, 0, 0, ZoneId.of("UTC")), next);
    }

    // --- Empty times returns empty ---

    @Test
    void emptyTimesReturnsEmpty() {
        ZonedDateTime now = ZonedDateTime.now();
        assertTrue(RestartScheduleCalculator.calculateNextRestart(now, List.of(), List.of("ALL")).isEmpty());
    }

    // --- Null times returns empty ---

    @Test
    void nullTimesReturnsEmpty() {
        ZonedDateTime now = ZonedDateTime.now();
        assertTrue(RestartScheduleCalculator.calculateNextRestart(now, null, List.of("ALL")).isEmpty());
    }

    // --- Empty days returns empty ---

    @Test
    void emptyDaysReturnsEmpty() {
        ZonedDateTime now = ZonedDateTime.now();
        assertTrue(RestartScheduleCalculator.calculateNextRestart(now, List.of("12:00"), List.of()).isEmpty());
    }

    // --- Null days returns empty ---

    @Test
    void nullDaysReturnsEmpty() {
        ZonedDateTime now = ZonedDateTime.now();
        assertTrue(RestartScheduleCalculator.calculateNextRestart(now, List.of("12:00"), null).isEmpty());
    }

    // --- Single day that has already passed this week ---

    @Test
    void singleDayAlreadyPassedThisWeek() {
        // Monday May 25 2026 is a Monday
        // If today is Monday at 20:00 and the restart is at 08:00 on Monday only,
        // it should roll to next Monday
        ZonedDateTime now = ZonedDateTime.of(2026, 5, 25, 20, 0, 0, 0, ZoneId.of("UTC"));

        // Verify May 25 2026 is a Monday
        assertEquals(DayOfWeek.MONDAY, now.getDayOfWeek());

        ZonedDateTime next = RestartScheduleCalculator.calculateNextRestart(
            now,
            List.of("08:00"),
            List.of("MONDAY")
        ).orElseThrow();

        // Should be next Monday at 08:00
        assertEquals(DayOfWeek.MONDAY, next.getDayOfWeek());
        assertTrue(next.isAfter(now));
        assertEquals(8, next.getHour());
    }

    // --- "ALL" days covers every day ---

    @Test
    void allDaysCoversEveryDay() {
        Set<DayOfWeek> days = RestartScheduleCalculator.parseDays(List.of("ALL"));
        assertEquals(7, days.size(), "ALL should cover all 7 days");
    }

    // --- Case-insensitive day names ---

    @Test
    void dayNamesAreCaseInsensitive() {
        Set<DayOfWeek> upper = RestartScheduleCalculator.parseDays(List.of("MONDAY"));
        Set<DayOfWeek> lower = RestartScheduleCalculator.parseDays(List.of("monday"));
        Set<DayOfWeek> mixed = RestartScheduleCalculator.parseDays(List.of("Monday"));

        assertEquals(upper, lower);
        assertEquals(upper, mixed);
        assertTrue(upper.contains(DayOfWeek.MONDAY));
    }

    // --- Invalid day names are ignored ---

    @Test
    void invalidDayNamesAreIgnored() {
        Set<DayOfWeek> days = RestartScheduleCalculator.parseDays(List.of("MONDAY", "NOTADAY", "FRIDAY"));
        assertEquals(2, days.size());
        assertTrue(days.contains(DayOfWeek.MONDAY));
        assertTrue(days.contains(DayOfWeek.FRIDAY));
    }

    // --- parseTime handles various formats ---

    @Test
    void parseTimeHandles24HourFormat() {
        assertEquals(14 * 60, RestartScheduleCalculator.parseTime("14:00")
            .map(t -> t.getHour() * 60 + t.getMinute())
            .orElse(-1));
    }

    @Test
    void parseTimeHandlesSingleDigitHour() {
        assertEquals(6 * 60, RestartScheduleCalculator.parseTime("6:00")
            .map(t -> t.getHour() * 60 + t.getMinute())
            .orElse(-1));
    }

    @Test
    void parseTimeRejectsInvalidFormat() {
        assertTrue(RestartScheduleCalculator.parseTime("25:00").isEmpty(),
            "25:00 should be rejected");
        assertTrue(RestartScheduleCalculator.parseTime("abc").isEmpty(),
            "Non-numeric time should be rejected");
        assertTrue(RestartScheduleCalculator.parseTime("").isEmpty(),
            "Empty time should be rejected");
        assertTrue(RestartScheduleCalculator.parseTime(null).isEmpty(),
            "Null time should be rejected");
    }

    // --- Timezone-aware calculation ---

    @Test
    void timezoneAwareCalculation() {
        // 12:00 UTC = 17:30 IST
        ZonedDateTime nowIST = ZonedDateTime.of(2026, 5, 25, 17, 30, 0, 0, ZoneId.of("Asia/Kolkata"));

        ZonedDateTime next = RestartScheduleCalculator.calculateNextRestart(
            nowIST,
            List.of("18:00"),
            List.of("ALL")
        ).orElseThrow();

        // 18:00 IST is the next restart
        assertEquals(ZoneId.of("Asia/Kolkata"), next.getZone());
        assertEquals(18, next.getHour());
        assertEquals(0, next.getMinute());
    }
}
