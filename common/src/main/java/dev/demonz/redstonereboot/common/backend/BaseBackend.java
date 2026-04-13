package dev.demonz.redstonereboot.common.backend;

import java.util.logging.Logger;

/**
 * Base implementation for all restart backends.
 */
public abstract class BaseBackend implements RestartBackend {

    protected final Logger logger;
    private final String name;

    protected BaseBackend(Logger logger, String name) {
        this.logger = logger;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void prepare() {
        // Default: no preparation needed
    }

    @Override
    public abstract BackendResult execute();

    @Override
    public abstract BackendState getState();

    @Override
    public abstract boolean isControllerOwned();
}
