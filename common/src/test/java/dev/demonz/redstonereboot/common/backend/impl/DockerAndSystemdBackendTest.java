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


    @Test
    void dockerBackendIsNotControllerOwned() {
        DockerBackend backend = new DockerBackend(logger);
        assertFalse(backend.isControllerOwned(),
            "Docker backend should NOT be controller-owned");
    }

    @Test
    void dockerBackendStateOnNonDockerHost() {
        DockerBackend backend = new DockerBackend(logger);
        RestartBackend.BackendState state = backend.getState();
        assertTrue(state == RestartBackend.BackendState.MISCONFIGURED
                || state == RestartBackend.BackendState.ASSISTED
                || state == RestartBackend.BackendState.FULL,
            "Docker state should be valid, got " + state);
    }

    @Test
    void dockerBackendExecuteOnNonDockerHost() {
        DockerBackend backend = new DockerBackend(logger);
        BackendResult result = backend.execute();
        assertTrue(result == BackendResult.FAILED || result == BackendResult.ACCEPTED,
            "Docker execute should return FAILED or ACCEPTED, got " + result);
    }

    @Test
    void dockerBackendCleanupDoesNotThrow() {
        DockerBackend backend = new DockerBackend(logger);
        backend.cleanup();
    }


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