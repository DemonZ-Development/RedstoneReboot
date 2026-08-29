package dev.demonz.redstonereboot.common.backend.impl;

import dev.demonz.redstonereboot.common.backend.BackendResult;
import dev.demonz.redstonereboot.common.backend.RestartBackend;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PterodactylBackendTest {

    private final Logger logger = Logger.getLogger("PterodactylBackendTest");

    @Test
    void constructorRejectsUrlWithoutScheme() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            new PterodactylBackend(logger, "panel.example.com", "key", "id"));
        assertTrue(ex.getMessage().contains("http://") || ex.getMessage().contains("https://"),
            "Error message should mention required scheme");
    }

    @Test
    void constructorAcceptsHttpsUrl() {
        PterodactylBackend backend = new PterodactylBackend(logger,
            "https://panel.example.com", "key123", "server1");
        assertEquals("Pterodactyl", backend.getName());
    }

    @Test
    void constructorAcceptsHttpUrl() {
        PterodactylBackend backend = new PterodactylBackend(logger,
            "http://panel.example.com", "key123", "server1");
        assertEquals("Pterodactyl", backend.getName());
    }

    @Test
    void executeReturnsFailedWhenUrlIsBlank() {
        PterodactylBackend backend = new PterodactylBackend(logger,
            "", "key123", "server1");
        assertEquals(BackendResult.FAILED, backend.execute());
    }

    @Test
    void executeReturnsFailedWhenApiKeyIsBlank() {
        PterodactylBackend backend = new PterodactylBackend(logger,
            "https://panel.example.com", "", "server1");
        assertEquals(BackendResult.FAILED, backend.execute());
    }

    @Test
    void executeReturnsFailedWhenServerIdIsBlank() {
        PterodactylBackend backend = new PterodactylBackend(logger,
            "https://panel.example.com", "key123", "");
        assertEquals(BackendResult.FAILED, backend.execute());
    }

    @Test
    void executeReturnsFailedWhenAllBlank() {
        PterodactylBackend backend = new PterodactylBackend(logger,
            "", "", "");
        assertEquals(BackendResult.FAILED, backend.execute());
    }

    @Test
    void isControllerOwnedReturnsTrue() {
        PterodactylBackend backend = new PterodactylBackend(logger,
            "https://panel.example.com", "key", "id");
        assertTrue(backend.isControllerOwned(),
            "Pterodactyl backend should be controller-owned");
    }

    @Test
    void getStateReturnsMisconfiguredWhenUrlIsBlank() {
        PterodactylBackend backend = new PterodactylBackend(logger,
            "", "key", "id");
        assertEquals(RestartBackend.BackendState.MISCONFIGURED, backend.getState());
    }

    @Test
    void getStateReturnsMisconfiguredWhenKeyIsBlank() {
        PterodactylBackend backend = new PterodactylBackend(logger,
            "https://panel.example.com", "", "id");
        assertEquals(RestartBackend.BackendState.MISCONFIGURED, backend.getState());
    }

    @Test
    void toStringMasksApiKey() {
        PterodactylBackend backend = new PterodactylBackend(logger,
            "https://panel.example.com", "super_secret_key_12345", "server1");
        String str = backend.toString();
        assertTrue(str.contains("***MASKED***"), "toString should mask the API key");
        assertFalse(str.contains("super_secret_key_12345"),
            "toString should NOT contain the actual API key");
    }

    @Test
    void nullServerIdHandledGracefully() {
        PterodactylBackend backend = new PterodactylBackend(logger,
            "https://panel.example.com", "key", null);
        assertEquals(BackendResult.FAILED, backend.execute());
        assertEquals(RestartBackend.BackendState.MISCONFIGURED, backend.getState());
    }

    @Test
    void cleanupDoesNotThrow() {
        PterodactylBackend backend = new PterodactylBackend(logger,
            "https://panel.example.com", "key", "id");
        backend.cleanup();
    }

    @Test
    void executeConnectionRefusedReturnsFailedOrUnknown() {
        PterodactylBackend backend = new PterodactylBackend(logger,
            "http://127.0.0.1:1", "key123", "server1");
        BackendResult result = backend.execute();
        assertTrue(result == BackendResult.FAILED || result == BackendResult.UNKNOWN,
            "Connection to non-existent host should return FAILED or UNKNOWN, got " + result);
    }
}
