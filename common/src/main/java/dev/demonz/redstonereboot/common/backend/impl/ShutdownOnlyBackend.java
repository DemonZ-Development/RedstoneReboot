package dev.demonz.redstonereboot.common.backend.impl;

import dev.demonz.redstonereboot.common.backend.BackendResult;
import dev.demonz.redstonereboot.common.backend.SupervisorBackend;

import java.util.logging.Logger;

/**
 * Fallback backend that only performs a graceful shutdown.
 */
public class ShutdownOnlyBackend extends SupervisorBackend {

    public ShutdownOnlyBackend(Logger logger) {
        super(logger, "ShutdownOnly");
    }

    @Override
    public BackendResult execute() {
        // Successful because it allows the local shutdown sequence to proceed.
        return BackendResult.ACCEPTED;
    }

    @Override
    public BackendState getState() {
        return BackendState.SHUTDOWN_ONLY;
    }
}
