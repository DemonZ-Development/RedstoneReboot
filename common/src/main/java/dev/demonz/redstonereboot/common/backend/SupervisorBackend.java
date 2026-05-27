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

    /**
     * Check whether this supervisor backend is properly "wired" into the server's
     * startup process. Wiring is proven by either the {@code redstonereboot.active}
     * system property or the {@code REDSTONEREBOOT_ACTIVE} environment variable.
     *
     * @return {@code true} if the backend is wired into the startup command
     */
    protected boolean isWired() {
        return Boolean.getBoolean("redstonereboot.active")
            || "1".equals(System.getenv("REDSTONEREBOOT_ACTIVE"));
    }
}
