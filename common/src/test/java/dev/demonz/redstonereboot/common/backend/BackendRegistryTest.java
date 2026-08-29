package dev.demonz.redstonereboot.common.backend;

import dev.demonz.redstonereboot.common.backend.impl.ShutdownOnlyBackend;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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
