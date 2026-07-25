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
import dev.demonz.redstonereboot.common.backend.SupervisorBackend;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Backend for servers running inside Docker containers.
 */
public class DockerBackend extends SupervisorBackend {

    public DockerBackend(Logger logger) {
        super(logger, "Docker");
    }

    @Override
    public BackendResult execute() {
        if (!isDockerEnvironment()) {
            logger.warning("Docker backend executed outside a Docker environment.");
            return BackendResult.FAILED;
        }

        if (!isWired()) {
            logger.warning("Docker backend executed but not wired! Ensure your container has a restart policy.");
            return BackendResult.FAILED;
        }
        return BackendResult.ACCEPTED;
    }

    @Override
    public BackendState getState() {
        if (!isDockerEnvironment()) {
            return BackendState.MISCONFIGURED;
        }
        if (isWired()) {
            return BackendState.FULL;
        }
        return BackendState.ASSISTED;
    }

    private boolean isDockerEnvironment() {
        try {
            return Files.exists(Paths.get("/.dockerenv"));
        } catch (SecurityException exception) {
            logger.warning("SecurityManager blocked Docker environment check: " + exception.getMessage());
            return false;
        }
    }
}