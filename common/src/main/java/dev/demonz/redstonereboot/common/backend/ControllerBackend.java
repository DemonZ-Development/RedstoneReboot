package dev.demonz.redstonereboot.common.backend;

import java.util.logging.Logger;

public abstract class ControllerBackend extends BaseBackend {

    protected ControllerBackend(Logger logger, String name) {
        super(logger, name);
    }

    @Override
    public final boolean isControllerOwned() {
        return true;
    }
}
