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

/**
 * Abstracts platform-specific task scheduling for the RedstoneReboot core engine.
 * <p>
 * Each platform (Bukkit, Folia, Fabric, Forge, NeoForge) provides an implementation
 * to bridge its native scheduler into the common engine's tick-based timing model.
 * All delay and period values are expressed in <b>server ticks</b> (1 tick = 50ms at 20 TPS).
 * </p>
 *
 * @see ScheduledTaskHandle
 * @since 1.0.0
 */
public interface PlatformTaskScheduler {

    /**
     * Schedule a task to run repeatedly at a fixed interval.
     *
     * @param task              the runnable to execute
     * @param initialDelayTicks initial delay before the first execution, in server ticks
     * @param periodTicks       interval between subsequent executions, in server ticks
     * @return a handle that can cancel the scheduled task
     */
    ScheduledTaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks);

    /**
     * Schedule a task to run repeatedly at a fixed interval asynchronously (off the server tick thread).
     *
     * @param task              the runnable to execute
     * @param initialDelayTicks initial delay before the first execution, in server ticks
     * @param periodTicks       interval between subsequent executions, in server ticks
     * @return a handle that can cancel the scheduled task
     */
    ScheduledTaskHandle runRepeatingAsync(Runnable task, long initialDelayTicks, long periodTicks);

    /**
     * Schedule a task to run once after a delay.
     *
     * @param task       the runnable to execute
     * @param delayTicks delay before execution, in server ticks
     * @return a handle that can cancel the scheduled task
     */
    ScheduledTaskHandle runLater(Runnable task, long delayTicks);

    /**
     * Schedule a task to run once after a delay asynchronously (off the server tick thread).
     *
     * @param task       the runnable to execute
     * @param delayTicks delay before execution, in server ticks
     * @return a handle that can cancel the scheduled task
     */
    ScheduledTaskHandle runLaterAsync(Runnable task, long delayTicks);

    /**
     * Check whether the current runtime is a Folia environment.
     *
     * @return {@code true} if running on a Folia-based server
     */
    boolean isFolia();
}
