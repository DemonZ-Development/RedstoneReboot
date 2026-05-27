package dev.demonz.redstonereboot.common.backend;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Practical tests for {@link EnvironmentDetector} — environment detection
 * and output validation.
 */
class EnvironmentDetectorTest {

    // --- detectPotentialBackends does not crash ---

    @Test
    void detectDoesNotCrash() {
        List<String> result = EnvironmentDetector.detectPotentialBackends();
        assertNotNull(result, "Detection result should never be null");
    }

    // --- Detection result contains only known backend types ---

    @Test
    void detectionResultContainsOnlyKnownTypes() {
        List<String> result = EnvironmentDetector.detectPotentialBackends();
        for (String detected : result) {
            assertTrue(
                detected.equals("SYSTEMD")
                || detected.equals("DOCKER")
                || detected.equals("PTERODACTYL"),
                "Detected backend should be a known type, got: " + detected);
        }
    }

    // --- Pterodactyl detection via env var ---

    @Test
    void pterodactylDetectedViaEnvVar() {
        // If PTERODACTYL=1 is set in the environment, it should be detected
        // This test just verifies the detection path exists
        String pteroEnv = System.getenv("PTERODACTYL");
        List<String> result = EnvironmentDetector.detectPotentialBackends();
        if ("1".equals(pteroEnv)) {
            assertTrue(result.contains("PTERODACTYL"),
                "PTERODACTYL=1 should be detected");
        }
        // If not set, we just verify no crash
    }
}
