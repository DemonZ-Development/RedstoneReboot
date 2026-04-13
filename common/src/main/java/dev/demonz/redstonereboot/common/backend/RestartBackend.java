package dev.demonz.redstonereboot.common.backend;

/**
 * Interface for a restart execution backend.
 */
public interface RestartBackend {

    /**
     * Get the identifying name of this backend.
     */
    String getName();

    /**
     * Prepare for restart (e.g., ensure scripts are generated or connections alive).
     * Called at the start of the countdown.
     */
    void prepare();

    /**
     * Execute the restart logic.
     *
     * @return the result of the execution attempt
     */
    BackendResult execute();

    /**
     * Get the current diagnostic state of the backend.
     */
    BackendState getState();

    /**
     * @return true if this backend handles both shutdown and startup (no local shutdown needed).
     */
    boolean isControllerOwned();

    /**
     * Verification states for the 'doctor' diagnostic tool.
     */
    enum BackendState {
        /** Backend is configured, wired, and verified. */
        FULL,
        /** Configured but verification (API/Connectivity) failed. */
        ASSISTED,
        /** Script/Service generated but not 'wired' into the startup command. */
        GENERATED,
        /** No auto-restart backend active; graceful shutdown only. */
        SHUTDOWN_ONLY,
        /** Critical configuration missing. */
        MISCONFIGURED
    }
}
