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

    private boolean isWired() {
        return Boolean.getBoolean("redstonereboot.active") || 
               "1".equals(System.getenv("REDSTONEREBOOT_ACTIVE"));
    }

    private boolean isDockerEnvironment() {
        return Files.exists(Paths.get("/.dockerenv"));
    }
}
