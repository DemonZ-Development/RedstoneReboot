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


package dev.demonz.redstonereboot.common.scheduler;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Standard Java-based scheduler for platforms without a native tick-based scheduler (Fabric/Forge).
 */
public class JavaPlatformScheduler implements PlatformTaskScheduler {

    private static final Logger LOGGER = Logger.getLogger(JavaPlatformScheduler.class.getName());

    private final ScheduledExecutorService executor;
    private final Executor dispatcher;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public JavaPlatformScheduler() {
        this(Runnable::run);
    }

    public JavaPlatformScheduler(Executor dispatcher) {
        this.dispatcher = dispatcher != null ? dispatcher : Runnable::run;
        this.executor = Executors.newSingleThreadScheduledExecutor(new SchedulerThreadFactory());
    }

    @Override
    public ScheduledTaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(task, "task must not be null");
        if (shutdown.get()) {
            LOGGER.warning("runRepeating called after shutdown — task discarded.");
            return () -> {};
        }
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(
            () -> dispatchSafely(task),
            initialDelayTicks * 50,
            periodTicks * 50,
            TimeUnit.MILLISECONDS
        );
        return () -> future.cancel(false);
    }

    @Override
    public ScheduledTaskHandle runRepeatingAsync(Runnable task, long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(task, "task must not be null");
        if (shutdown.get()) {
            LOGGER.warning("runRepeatingAsync called after shutdown — task discarded.");
            return () -> {};
        }
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(
            () -> runSafely(task),
            initialDelayTicks * 50,
            periodTicks * 50,
            TimeUnit.MILLISECONDS
        );
        return () -> future.cancel(false);
    }

    @Override
    public ScheduledTaskHandle runLater(Runnable task, long delayTicks) {
        Objects.requireNonNull(task, "task must not be null");
        if (shutdown.get()) {
            LOGGER.warning("runLater called after shutdown — task discarded.");
            return () -> {};
        }
        ScheduledFuture<?> future = executor.schedule(
            () -> dispatchSafely(task),
            delayTicks * 50,
            TimeUnit.MILLISECONDS
        );
        return () -> future.cancel(false);
    }

    @Override
    public ScheduledTaskHandle runLaterAsync(Runnable task, long delayTicks) {
        Objects.requireNonNull(task, "task must not be null");
        if (shutdown.get()) {
            LOGGER.warning("runLaterAsync called after shutdown — task discarded.");
            return () -> {};
        }
        ScheduledFuture<?> future = executor.schedule(
            () -> runSafely(task),
            delayTicks * 50,
            TimeUnit.MILLISECONDS
        );
        return () -> future.cancel(false);
    }

    @Override
    public boolean isFolia() {
        return false;
    }

    public void shutdown() {
        shutdown.set(true);
        executor.shutdownNow();
    }

    private void dispatchSafely(Runnable task) {
        if (shutdown.get()) return;
        try {
            dispatcher.execute(() -> runSafely(task));
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to dispatch scheduled task.", exception);
        }
    }

    private void runSafely(Runnable task) {
        if (shutdown.get()) return;
        try {
            task.run();
        } catch (Error error) {
            LOGGER.log(Level.SEVERE, "Scheduled task threw an Error.", error);
            throw error;
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Scheduled task failed.", exception);
        }
    }

    private static final class SchedulerThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "RedstoneReboot-Scheduler");
            thread.setDaemon(true);
            return thread;
        }
    }
}