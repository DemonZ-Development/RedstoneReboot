package dev.demonz.redstonereboot.common.utils;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class HalloweenEasterEgg implements AutoCloseable {
    private final Clock clock;
    private final ScheduledExecutorService executor;
    private final Consumer<String> console;
    private final Runnable notifyOperators;
    private final Set<UUID> greetedPlayers = new HashSet<>();
    private ScheduledFuture<?> midnightTask;
    private boolean running;
    private boolean closed;
    private int announcedYear = -1;
    private int greetedYear = -1;

    public HalloweenEasterEgg(String name, Consumer<String> console, Runnable notifyOperators) {
        this(Clock.systemDefaultZone(), Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, name + "-Halloween");
            thread.setDaemon(true);
            return thread;
        }), console, notifyOperators);
    }

    HalloweenEasterEgg(Clock clock, ScheduledExecutorService executor,
                      Consumer<String> console, Runnable notifyOperators) {
        this.clock = clock;
        this.executor = executor;
        this.console = console;
        this.notifyOperators = notifyOperators;
    }

    public synchronized void start() {
        if (running || closed) return;
        running = true;
        checkDate(true);
        executor.scheduleWithFixedDelay(() -> checkDate(false), 7, 7, TimeUnit.HOURS);
    }

    private synchronized void checkDate(boolean startup) {
        if (!running) return;
        LocalDate today = LocalDate.now(clock);
        if (today.getMonth() == Month.OCTOBER) {
            int days = 31 - today.getDayOfMonth();
            if (days == 0) {
                if (announcedYear != today.getYear()) {
                    announcedYear = today.getYear();
                    console.accept("Happy Halloween!");
                }
                try {
                    notifyOperators.run();
                } catch (RuntimeException failure) {
                    console.accept("Could not deliver Halloween greeting: " + failure.getMessage());
                }
            } else if (startup) {
                console.accept("Halloween in " + days + (days == 1 ? " day!" : " days!"));
            }
        }
        scheduleMidnight();
    }

    private void scheduleMidnight() {
        if (midnightTask != null) midnightTask.cancel(false);
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime midnight = LocalDate.of(now.getYear(), Month.OCTOBER, 31)
            .atStartOfDay(clock.getZone());
        if (!midnight.isAfter(now)) {
            midnight = LocalDate.of(now.getYear() + 1, Month.OCTOBER, 31)
                .atStartOfDay(clock.getZone());
        }
        long delay = Math.max(1, Duration.between(now, midnight).toMillis());
        midnightTask = executor.schedule(() -> checkDate(false), delay, TimeUnit.MILLISECONDS);
    }

    public synchronized void greetPlayer(UUID playerId, Consumer<String> sendMessage) {
        if (!running) return;
        LocalDate today = LocalDate.now(clock);
        if (today.getMonth() != Month.OCTOBER || today.getDayOfMonth() != 31) return;
        if (greetedYear != today.getYear()) {
            greetedYear = today.getYear();
            greetedPlayers.clear();
        }
        if (greetedPlayers.contains(playerId)) return;
        sendMessage.accept("Happy Halloween!");
        greetedPlayers.add(playerId);
    }

    @Override
    public synchronized void close() {
        running = false;
        closed = true;
        executor.shutdownNow();
        greetedPlayers.clear();
    }
}
