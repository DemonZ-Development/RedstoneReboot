package dev.demonz.redstonereboot.common.backend;

import java.util.logging.Logger;

public abstract class SupervisorBackend extends BaseBackend {

    protected SupervisorBackend(Logger logger, String name) {
        super(logger, name);
    }

    @Override
    public final boolean isControllerOwned() {
        return false;
    }

    protected boolean isWired() {
        return Boolean.getBoolean("redstonereboot.active")
            || "1".equals(System.getenv("REDSTONEREBOOT_ACTIVE"));
    }
}
