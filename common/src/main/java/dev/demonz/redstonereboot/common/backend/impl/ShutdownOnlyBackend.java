package dev.demonz.redstonereboot.common.backend.impl;

import dev.demonz.redstonereboot.common.backend.BackendResult;
import dev.demonz.redstonereboot.common.backend.SupervisorBackend;

import java.util.logging.Logger;

public class ShutdownOnlyBackend extends SupervisorBackend {

    public ShutdownOnlyBackend(Logger logger) {
        super(logger, "ShutdownOnly");
    }

    @Override
    public BackendResult execute() {
        return BackendResult.ACCEPTED;
    }

    @Override
    public BackendState getState() {
        return BackendState.DEPEND_ON_HOST;
    }
}
