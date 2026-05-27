package dev.demonz.redstonereboot.common.backend.impl;

import dev.demonz.redstonereboot.common.backend.BackendResult;
import dev.demonz.redstonereboot.common.backend.RestartBackend;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Practical tests for Docker and Systemd backends — environment detection,
 * wiring checks, and state behavior.
 */
class DockerAndSystemdBackendTest {

    private final Logger logger = Logger.getLogger("DockerAndSystemdBackendTest");

    // --- Docker Backend ---

    @Test
    void dockerBackendIsNotControllerOwned() {
        DockerBackend backend = new DockerBackend(logger);
        assertFalse(backend.isControllerOwned(),
            "Docker backend should NOT be controller-owned");
    }

    @Test
    void dockerBackendStateOnNonDockerHost() {
        DockerBackend backend = new DockerBackend(logger);
        // If we're not in Docker (/.dockerenv doesn't exist), state should be MISCONFIGURED
        RestartBackend.BackendState state = backend.getState();
        // On a dev machine, either MISCONFIGURED (not Docker) or ASSISTED/FULL (if Docker)
        // We just verify it returns a valid state
        assertTrue(state == RestartBackend.BackendState.MISCONFIGURED
                || state == RestartBackend.BackendState.ASSISTED
                || state == RestartBackend.BackendState.FULL,
            "Docker state should be valid, got " + state);
    }

    @Test
    void dockerBackendExecuteOnNonDockerHost() {
        DockerBackend backend = new DockerBackend(logger);
        BackendResult result = backend.execute();
        // If not in Docker, should return FAILED
        // If somehow in Docker, could return ACCEPTED or FAILED depending on wiring
        assertTrue(result == BackendResult.FAILED || result == BackendResult.ACCEPTED,
            "Docker execute should return FAILED or ACCEPTED, got " + result);
    }

    @Test
    void dockerBackendCleanupDoesNotThrow() {
        DockerBackend backend = new DockerBackend(logger);
        backend.cleanup();
    }

    // --- Systemd Backend ---

    @Test
    void systemdBackendIsNotControllerOwned() {
        SystemdBackend backend = new SystemdBackend(logger, "minecraft");
        assertFalse(backend.isControllerOwned(),
            "Systemd backend should NOT be controller-owned");
    }

    @Test
    void systemdBackendStateOnNonSystemdHost() {
        SystemdBackend backend = new SystemdBackend(logger, "minecraft");
        RestartBackend.BackendState state = backend.getState();
        // On a dev machine without systemd, should be MISCONFIGURED
        // On a machine with systemd, could be ASSISTED/FULL
        assertTrue(state == RestartBackend.BackendState.MISCONFIGURED
                || state == RestartBackend.BackendState.ASSISTED
                || state == RestartBackend.BackendState.FULL,
            "Systemd state should be valid, got " + state);
    }

    @Test
    void systemdBackendExecuteOnNonSystemdHost() {
        SystemdBackend backend = new SystemdBackend(logger, "minecraft");
        BackendResult result = backend.execute();
        assertTrue(result == BackendResult.FAILED || result == BackendResult.ACCEPTED,
            "Systemd execute should return FAILED or ACCEPTED, got " + result);
    }

    @Test
    void systemdBackendCleanupDoesNotThrow() {
        SystemdBackend backend = new SystemdBackend(logger, "minecraft");
        backend.cleanup();
    }

    private static void assertTrue(boolean condition, String message) {
        org.junit.jupiter.api.Assertions.assertTrue(condition, message);
    }
}
