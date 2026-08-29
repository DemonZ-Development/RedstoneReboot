package dev.demonz.redstonereboot.common.backend;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentDetectorTest {

    @Test
    void detectDoesNotCrash() {
        List<String> result = EnvironmentDetector.detectPotentialBackends();
        assertNotNull(result, "Detection result should never be null");
    }

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

    @Test
    void pterodactylDetectedViaEnvVar() {
        String pteroEnv = System.getenv("PTERODACTYL");
        List<String> result = EnvironmentDetector.detectPotentialBackends();
        if ("1".equals(pteroEnv)) {
            assertTrue(result.contains("PTERODACTYL"),
                "PTERODACTYL=1 should be detected");
        }
    }
}
