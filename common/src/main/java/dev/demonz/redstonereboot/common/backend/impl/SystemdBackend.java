package dev.demonz.redstonereboot.common.backend.impl;

import dev.demonz.redstonereboot.common.backend.BackendResult;
import dev.demonz.redstonereboot.common.backend.SupervisorBackend;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Backend for servers running under Systemd.
 */
public class SystemdBackend extends SupervisorBackend {

    private final String serviceName;

    public SystemdBackend(Logger logger, String serviceName) {
        super(logger, "Systemd");
        this.serviceName = serviceName;
    }

    @Override
    public BackendResult execute() {
        if (!isSystemdEnvironment()) {
            logger.warning("Systemd backend executed outside a systemd-managed environment.");
            return BackendResult.FAILED;
        }

        if (!isWired()) {
            logger.warning("Systemd backend executed but not wired! Use Restart=always in your .service file.");
            return BackendResult.FAILED;
        }
        return BackendResult.ACCEPTED;
    }

    @Override
    public BackendState getState() {
        if (!isSystemdEnvironment()) {
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

    private boolean isSystemdEnvironment() {
        return Files.exists(Paths.get("/run/systemd/system"));
    }
}
