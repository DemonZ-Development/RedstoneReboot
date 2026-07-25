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


package dev.demonz.redstonereboot.common.backend;

import dev.demonz.redstonereboot.common.backend.impl.ShutdownOnlyBackend;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link BackendRegistry} — fallback behavior, null backend handling,
 * and disabled-by-default backend configuration.
 */
class BackendRegistryTest {

    @TempDir
    Path tempDir;

    @Test
    void getActiveBackendReturnsFallbackWhenNotInitialized() {
        Logger logger = Logger.getLogger("BackendRegistryTest");
        BackendConfig config = new BackendConfig(tempDir, logger);
        BackendRegistry registry = new BackendRegistry(logger, config, tempDir);

        RestartBackend backend = registry.getActiveBackend();
        assertNotNull(backend);
        assertEquals("ShutdownOnly", backend.getName());
    }

    @Test
    void fallbackBackendIsCached() {
        Logger logger = Logger.getLogger("BackendRegistryTest");
        BackendConfig config = new BackendConfig(tempDir, logger);
        BackendRegistry registry = new BackendRegistry(logger, config, tempDir);

        RestartBackend first = registry.getActiveBackend();
        RestartBackend second = registry.getActiveBackend();
        assertSame(first, second, "Fallback backend should be cached, not re-created each call");
    }

    @Test
    void initializeWithBackendsDisabledUsesShutdownOnly() {
        Logger logger = Logger.getLogger("BackendRegistryTest");
        BackendConfig config = new BackendConfig(tempDir, logger);
        BackendRegistry registry = new BackendRegistry(logger, config, tempDir);

        registry.initialize();

        RestartBackend backend = registry.getActiveBackend();
        assertNotNull(backend);
        assertEquals("ShutdownOnly", backend.getName());
    }

    @Test
    void initializeWithLoadFailureFallsBackToShutdownOnly() {
        Logger logger = Logger.getLogger("BackendRegistryTest");
        BackendConfig config = new BackendConfig(
            Path.of("/proc/nonexistent-path-that-will-fail"), logger);
        BackendRegistry registry = new BackendRegistry(logger, config, tempDir);

        registry.initialize();

        RestartBackend backend = registry.getActiveBackend();
        assertNotNull(backend);
        assertEquals("ShutdownOnly", backend.getName());
    }

    @Test
    void shutdownOnlyBackendIsNotControllerOwned() {
        Logger logger = Logger.getLogger("BackendRegistryTest");
        BackendConfig config = new BackendConfig(tempDir, logger);
        BackendRegistry registry = new BackendRegistry(logger, config, tempDir);
        registry.initialize();

        RestartBackend backend = registry.getActiveBackend();
        assertEquals(false, backend.isControllerOwned(),
            "ShutdownOnlyBackend should not be controller-owned");
    }

    @Test
    void shutdownOnlyBackendReturnsAccepted() {
        Logger logger = Logger.getLogger("BackendRegistryTest");
        ShutdownOnlyBackend backend = new ShutdownOnlyBackend(logger);
        assertEquals(BackendResult.ACCEPTED, backend.execute());
    }

    @Test
    void shutdownOnlyBackendStateIsShutdownOnly() {
        Logger logger = Logger.getLogger("BackendRegistryTest");
        ShutdownOnlyBackend backend = new ShutdownOnlyBackend(logger);
        assertEquals(RestartBackend.BackendState.DEPEND_ON_HOST, backend.getState());
    }
}