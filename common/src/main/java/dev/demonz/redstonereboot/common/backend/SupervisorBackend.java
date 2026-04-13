package dev.demonz.redstonereboot.common.backend;

import java.util.logging.Logger;

/**
 * A backend where the plugin must perform a local shutdown after arming the supervisor.
 */
public abstract class SupervisorBackend extends BaseBackend {

    protected SupervisorBackend(Logger logger, String name) {
        super(logger, name);
    }

    @Override
    public final boolean isControllerOwned() {
        return false;
    }
}
