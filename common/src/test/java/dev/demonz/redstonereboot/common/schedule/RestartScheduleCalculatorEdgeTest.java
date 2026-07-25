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


    @Test
    void emptyTimesReturnsEmpty() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        assertTrue(RestartScheduleCalculator.calculateNextRestart(now, List.of(), List.of("ALL")).isEmpty());
    }


    @Test
    void nullTimesReturnsEmpty() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        assertTrue(RestartScheduleCalculator.calculateNextRestart(now, null, List.of("ALL")).isEmpty());
    }


    @Test
    void emptyDaysReturnsEmpty() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        assertTrue(RestartScheduleCalculator.calculateNextRestart(now, List.of("12:00"), List.of()).isEmpty());
    }


    @Test
    void nullDaysReturnsEmpty() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        assertTrue(RestartScheduleCalculator.calculateNextRestart(now, List.of("12:00"), null).isEmpty());
    }


    @Test
    void singleDayAlreadyPassedThisWeek() {
        ZonedDateTime now = ZonedDateTime.of(2026, 5, 25, 20, 0, 0, 0, ZoneId.of("UTC"));

        assertEquals(DayOfWeek.MONDAY, now.getDayOfWeek());

        ZonedDateTime next = RestartScheduleCalculator.calculateNextRestart(
            now,
            List.of("08:00"),
            List.of("MONDAY")
        ).orElseThrow();

        assertEquals(DayOfWeek.MONDAY, next.getDayOfWeek());
        assertTrue(next.isAfter(now));
        assertEquals(8, next.getHour());
    }


    @Test
    void allDaysCoversEveryDay() {
        Set<DayOfWeek> days = RestartScheduleCalculator.parseDays(List.of("ALL"));
        assertEquals(7, days.size(), "ALL should cover all 7 days");
    }


    @Test
    void dayNamesAreCaseInsensitive() {
        Set<DayOfWeek> upper = RestartScheduleCalculator.parseDays(List.of("MONDAY"));
        Set<DayOfWeek> lower = RestartScheduleCalculator.parseDays(List.of("monday"));
        Set<DayOfWeek> mixed = RestartScheduleCalculator.parseDays(List.of("Monday"));

        assertEquals(upper, lower);
        assertEquals(upper, mixed);
        assertTrue(upper.contains(DayOfWeek.MONDAY));
    }


    @Test
    void invalidDayNamesAreIgnored() {
        Set<DayOfWeek> days = RestartScheduleCalculator.parseDays(List.of("MONDAY", "NOTADAY", "FRIDAY"));
        assertEquals(2, days.size());
        assertTrue(days.contains(DayOfWeek.MONDAY));
        assertTrue(days.contains(DayOfWeek.FRIDAY));
    }


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


    @Test
    void timezoneAwareCalculation() {
        ZonedDateTime nowIST = ZonedDateTime.of(2026, 5, 25, 17, 30, 0, 0, ZoneId.of("Asia/Kolkata"));

        ZonedDateTime next = RestartScheduleCalculator.calculateNextRestart(
            nowIST,
            List.of("18:00"),
            List.of("ALL")
        ).orElseThrow();

        assertEquals(ZoneId.of("Asia/Kolkata"), next.getZone());
        assertEquals(18, next.getHour());
        assertEquals(0, next.getMinute());
    }
}