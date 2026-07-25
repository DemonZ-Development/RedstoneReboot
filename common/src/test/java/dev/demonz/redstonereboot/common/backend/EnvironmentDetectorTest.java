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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Practical tests for {@link EnvironmentDetector} — environment detection
 * and output validation.
 */
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