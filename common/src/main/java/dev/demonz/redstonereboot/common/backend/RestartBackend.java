package dev.demonz.redstonereboot.common.backend;

public interface RestartBackend {

    String getName();

    void prepare();

    BackendResult execute();

    BackendState getState();

    boolean isControllerOwned();

    default void cleanup() {}

    enum BackendState {

        FULL,

        ASSISTED,

        GENERATED,

        DEPEND_ON_HOST,

        @Deprecated
        SHUTDOWN_ONLY,

        MISCONFIGURED
    }
}
