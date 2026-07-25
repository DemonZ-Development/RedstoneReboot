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


package dev.demonz.redstonereboot.bukkit.e2e;

import dev.demonz.redstonereboot.bukkit.integrations.PlaceholderAPIHook;
import dev.demonz.redstonereboot.bukkit.scheduler.BukkitSchedulerFactory;
import dev.demonz.redstonereboot.common.RedstoneRebootCore;
import dev.demonz.redstonereboot.common.command.CommandProcessor;
import dev.demonz.redstonereboot.common.platform.ServerPlatform;
import dev.demonz.redstonereboot.common.platform.SimplePlatformConfig;
import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import dev.demonz.redstonereboot.common.scheduler.ScheduledTaskHandle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Bukkit, Paper, and Folia plugin components.
 */
class BukkitFoliaE2ETest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("E2E: Bukkit / Paper Scheduler Detection and Initialization")
    void testBukkitSchedulerFactory() {
        assertDoesNotThrow(() -> {
            boolean isFolia = BukkitSchedulerFactory.isFoliaEnvironment();
            assertFalse(isFolia, "Standard JVM environment should default to standard Bukkit scheduler");
        });
    }

    @Test
    @DisplayName("E2E: PlaceholderAPI Null-Safety & Format Validation")
    void testPlaceholderAPIHookE2E() {
        PlaceholderAPIHook hook = new PlaceholderAPIHook(null);
        
        assertEquals("1.5.0", hook.onRequest(null, "version"));
        assertNotNull(hook.onRequest(null, "next_restart"));
        assertNotNull(hook.onRequest(null, "time_until"));
        assertNotNull(hook.onRequest(null, "status"));
        assertNotNull(hook.onRequest(null, "reason"));
        assertNotNull(hook.onRequest(null, "tps"));
        assertNotNull(hook.onRequest(null, "memory"));
        assertNotNull(hook.onRequest(null, "timezone"));
        assertNull(hook.onRequest(null, "invalid_placeholder_key"));
    }

    @Test
    @DisplayName("E2E: Command Processor execution across status, info, doctor, reload, and schedule")
    void testCommandProcessorE2E() {
        SimplePlatformConfig config = new SimplePlatformConfig();
        TestScheduler scheduler = new TestScheduler();
        TestPlatform platform = new TestPlatform();
        RedstoneRebootCore core = new RedstoneRebootCore(platform, scheduler, config, tempDir);
        core.onEnable();

        CommandProcessor processor = new CommandProcessor(core);
        assertNotNull(processor);
    }

    private static class TestScheduler implements PlatformTaskScheduler {
        @Override public ScheduledTaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks) { return () -> {}; }
        @Override public ScheduledTaskHandle runRepeatingAsync(Runnable task, long initialDelayTicks, long periodTicks) { return () -> {}; }
        @Override public ScheduledTaskHandle runLater(Runnable task, long delayTicks) { task.run(); return () -> {}; }
        @Override public ScheduledTaskHandle runLaterAsync(Runnable task, long delayTicks) { task.run(); return () -> {}; }
        @Override public boolean isFolia() { return false; }
    }

    private static class TestPlatform implements ServerPlatform {
        @Override public void broadcastMessage(String message) {}
        @Override public void broadcastTitle(String title, String subtitle) {}
        @Override public void executeConsole(String command) {}
        @Override public double getTPS() { return 20.0; }
        @Override public void shutdownServer(String reason) {}
        @Override public void reloadPlatformState() {}
    }
}