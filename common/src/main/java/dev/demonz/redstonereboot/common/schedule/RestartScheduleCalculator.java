package dev.demonz.redstonereboot.common.schedule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Calculates the next configured restart window for a server.
 */
public final class RestartScheduleCalculator {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");

    private RestartScheduleCalculator() {
    }

    public static Optional<ZonedDateTime> calculateNextRestart(
        ZonedDateTime now,
        List<String> configuredTimes,
        List<String> configuredDays
    ) {
        Set<DayOfWeek> allowedDays = parseDays(configuredDays);
        List<LocalTime> times = configuredTimes.stream()
            .map(RestartScheduleCalculator::parseTime)
            .flatMap(Optional::stream)
            .sorted(Comparator.naturalOrder())
            .toList();

        if (allowedDays.isEmpty() || times.isEmpty()) {
            return Optional.empty();
        }

        for (int offset = 0; offset <= 7; offset++) {
            ZonedDateTime candidateDay = now.plusDays(offset);
            if (!allowedDays.contains(candidateDay.getDayOfWeek())) {
                continue;
            }

            for (LocalTime time : times) {
                ZonedDateTime candidate = candidateDay.with(time);
                if (offset == 0 && !candidate.isAfter(now)) {
                    continue;
                }
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    public static Optional<LocalTime> parseTime(String configuredTime) {
        if (configuredTime == null || configuredTime.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(LocalTime.parse(configuredTime.trim(), TIME_FORMAT));
        } catch (DateTimeParseException exception) {
            return Optional.empty();
        }
    }

    public static Set<DayOfWeek> parseDays(List<String> configuredDays) {
        if (configuredDays == null || configuredDays.isEmpty()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }

        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String configuredDay : configuredDays) {
            if (configuredDay == null || configuredDay.isBlank()) {
                continue;
            }

            String normalized = configuredDay.trim().toUpperCase(Locale.ROOT);
            if ("ALL".equals(normalized)) {
                return EnumSet.allOf(DayOfWeek.class);
            }

            try {
                days.add(DayOfWeek.valueOf(normalized));
            } catch (IllegalArgumentException ignored) {
                // Invalid values are ignored so platform-specific validation can
                // decide whether they are fatal.
            }
        }

        return days;
    }
}
