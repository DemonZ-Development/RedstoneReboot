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


package dev.demonz.redstonereboot.bukkit.scheduler;

import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import dev.demonz.redstonereboot.common.scheduler.ScheduledTaskHandle;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

final class BukkitTaskScheduler implements PlatformTaskScheduler {

    private final JavaPlugin plugin;

    BukkitTaskScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ScheduledTaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
        BukkitTask scheduled = Bukkit.getScheduler().runTaskTimer(
            plugin,
            () -> safelyRun(task),
            initialDelayTicks,
            periodTicks
        );
        return scheduled::cancel;
    }

    @Override
    public ScheduledTaskHandle runRepeatingAsync(Runnable task, long initialDelayTicks, long periodTicks) {
        BukkitTask scheduled = Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin,
            () -> safelyRun(task),
            initialDelayTicks,
            periodTicks
        );
        return scheduled::cancel;
    }

    @Override
    public ScheduledTaskHandle runLater(Runnable task, long delayTicks) {
        BukkitTask scheduled = Bukkit.getScheduler().runTaskLater(plugin, () -> safelyRun(task), delayTicks);
        return scheduled::cancel;
    }

    @Override
    public ScheduledTaskHandle runLaterAsync(Runnable task, long delayTicks) {
        BukkitTask scheduled = Bukkit.getScheduler().runTaskLaterAsynchronously(
            plugin,
            () -> safelyRun(task),
            delayTicks
        );
        return scheduled::cancel;
    }

    @Override
    public boolean isFolia() {
        return false;
    }

    private void safelyRun(Runnable task) {
        try {
            task.run();
        } catch (Throwable exception) {
            plugin.getLogger().log(Level.SEVERE, "Scheduled task failed.", exception);
        }
    }
}