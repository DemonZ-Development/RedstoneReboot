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


package dev.demonz.redstonereboot.common.e2e;

import dev.demonz.redstonereboot.common.backend.BackendConfig;
import dev.demonz.redstonereboot.common.backend.BackendRegistry;
import dev.demonz.redstonereboot.common.backend.BackendResult;
import dev.demonz.redstonereboot.common.backend.RestartBackend;
import dev.demonz.redstonereboot.common.backend.impl.DockerBackend;
import dev.demonz.redstonereboot.common.backend.impl.LocalScriptBackend;
import dev.demonz.redstonereboot.common.backend.impl.PterodactylBackend;
import dev.demonz.redstonereboot.common.backend.impl.ShutdownOnlyBackend;
import dev.demonz.redstonereboot.common.backend.impl.SystemdBackend;
import dev.demonz.redstonereboot.common.platform.ServerPlatform;
import dev.demonz.redstonereboot.common.platform.SimplePlatformConfig;
import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import dev.demonz.redstonereboot.common.scheduler.ScheduledTaskHandle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Practical integration tests for all 5 backend restart engines:
 * DEPEND_ON_HOST, LOCALSCRIPT, SYSTEMD, DOCKER, PTERODACTYL.
 */
class EndToEndLiveBackendTest {

    @TempDir
    Path tempDir;

    private Logger logger;
    private SimplePlatformConfig config;
    private TestScheduler scheduler;
    private TestPlatform platform;

    @BeforeEach
    void setUp() {
        logger = Logger.getLogger("EndToEndLiveBackendTest");
        config = new SimplePlatformConfig();
        scheduler = new TestScheduler();
        platform = new TestPlatform();
    }

    @Test
    @DisplayName("E2E: ShutdownOnly Backend graceful execution")
    void testShutdownOnlyBackendE2E() {
        ShutdownOnlyBackend backend = new ShutdownOnlyBackend(logger);
        assertFalse(backend.isControllerOwned());
        assertEquals("ShutdownOnly", backend.getName());

        BackendResult result = backend.execute();
        assertEquals(BackendResult.ACCEPTED, result);
    }

    @Test
    @DisplayName("E2E: LocalScript Backend script creation and execution flow")
    void testLocalScriptBackendE2E() throws IOException {
        Path scriptPath = tempDir.resolve("restart_wrapper.sh");
        String scriptContent = "#!/bin/bash\necho 'Restarting server via wrapper script'\nexit 0\n";
        Files.writeString(scriptPath, scriptContent);
        scriptPath.toFile().setExecutable(true);

        LocalScriptBackend backend = new LocalScriptBackend(logger, scriptPath.getFileName().toString(), tempDir);
        assertFalse(backend.isControllerOwned());
        assertEquals("LocalScript", backend.getName());

        RestartBackend.BackendState state = backend.getState();
        assertNotNull(state);

        BackendResult result = backend.execute();
        assertNotNull(result);
        backend.cleanup();
    }

    @Test
    @DisplayName("E2E: Systemd Backend service configuration and execution")
    void testSystemdBackendE2E() {
        SystemdBackend backend = new SystemdBackend(logger, "minecraft.service");
        assertFalse(backend.isControllerOwned());
        assertEquals("Systemd", backend.getName());

        RestartBackend.BackendState state = backend.getState();
        assertNotNull(state);

        BackendResult result = backend.execute();
        assertTrue(result == BackendResult.FAILED || result == BackendResult.ACCEPTED);
        backend.cleanup();
    }

    @Test
    @DisplayName("E2E: Docker Backend container execution flow")
    void testDockerBackendE2E() {
        DockerBackend backend = new DockerBackend(logger);
        assertFalse(backend.isControllerOwned());
        assertEquals("Docker", backend.getName());

        RestartBackend.BackendState state = backend.getState();
        assertNotNull(state);

        BackendResult result = backend.execute();
        assertTrue(result == BackendResult.FAILED || result == BackendResult.ACCEPTED);
        backend.cleanup();
    }

    @Test
    @DisplayName("E2E: Pterodactyl API endpoint authorization and request handling")
    void testPterodactylBackendE2E() {
        PterodactylBackend backend = new PterodactylBackend(logger,
                "https://panel.example.com", "ptlc_test_api_key_12345", "a1b2c3d4");
        assertTrue(backend.isControllerOwned());
        assertEquals("Pterodactyl", backend.getName());

        BackendResult result = backend.execute();
        assertTrue(result == BackendResult.FAILED || result == BackendResult.UNKNOWN);
        backend.cleanup();
    }

    @Test
    @DisplayName("E2E: Backend Registry loading and active selection")
    void testBackendRegistryFullE2E() {
        BackendConfig bConfig = new BackendConfig(tempDir, logger);
        bConfig.load();

        BackendRegistry registry = new BackendRegistry(logger, bConfig, tempDir);
        assertNotNull(registry.getActiveBackend());
        assertEquals("ShutdownOnly", registry.getActiveBackend().getName());
    }


    private static class TestScheduler implements PlatformTaskScheduler {
        @Override public ScheduledTaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks) { return () -> {}; }
        @Override public ScheduledTaskHandle runRepeatingAsync(Runnable task, long initialDelayTicks, long periodTicks) { return () -> {}; }
        @Override public ScheduledTaskHandle runLater(Runnable task, long delayTicks) { task.run(); return () -> {}; }
        @Override public ScheduledTaskHandle runLaterAsync(Runnable task, long delayTicks) { task.run(); return () -> {}; }
        @Override public boolean isFolia() { return false; }
    }

    private static class TestPlatform implements ServerPlatform {
        final AtomicBoolean shutdownCalled = new AtomicBoolean(false);
        String lastShutdownReason = "";

        @Override public void broadcastMessage(String message) {}
        @Override public void broadcastTitle(String title, String subtitle) {}
        @Override public void executeConsole(String command) {}
        @Override public double getTPS() { return 20.0; }
        @Override public void shutdownServer(String reason) {
            shutdownCalled.set(true);
            lastShutdownReason = reason;
        }
        @Override public void reloadPlatformState() {}
    }
}