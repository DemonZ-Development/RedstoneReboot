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

        // Before initialize(), activeBackend is null — should return fallback
        RestartBackend backend = registry.getActiveBackend();
        assertNotNull(backend);
        assertEquals("ShutdownOnly", backend.getName());
    }

    @Test
    void fallbackBackendIsCached() {
        Logger logger = Logger.getLogger("BackendRegistryTest");
        BackendConfig config = new BackendConfig(tempDir, logger);
        BackendRegistry registry = new BackendRegistry(logger, config, tempDir);

        // Calling getActiveBackend() twice should return the same cached instance
        RestartBackend first = registry.getActiveBackend();
        RestartBackend second = registry.getActiveBackend();
        assertSame(first, second, "Fallback backend should be cached, not re-created each call");
    }

    @Test
    void initializeWithBackendsDisabledUsesShutdownOnly() {
        Logger logger = Logger.getLogger("BackendRegistryTest");
        BackendConfig config = new BackendConfig(tempDir, logger);
        BackendRegistry registry = new BackendRegistry(logger, config, tempDir);

        // By default, backends-enabled=false in the generated properties file
        registry.initialize();

        RestartBackend backend = registry.getActiveBackend();
        assertNotNull(backend);
        assertEquals("ShutdownOnly", backend.getName());
    }

    @Test
    void initializeWithLoadFailureFallsBackToShutdownOnly() {
        Logger logger = Logger.getLogger("BackendRegistryTest");
        // Use a non-existent path deep enough that the parent dir creation will fail
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
        assertEquals(RestartBackend.BackendState.SHUTDOWN_ONLY, backend.getState());
    }
}
