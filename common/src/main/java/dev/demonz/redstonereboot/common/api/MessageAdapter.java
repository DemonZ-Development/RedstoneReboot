package dev.demonz.redstonereboot.common.api;

public interface MessageAdapter {

    void onMessage(MessageContext context);

    default String getName() {
        return getClass().getSimpleName();
    }

    default boolean handlesPostponed() { return true; }

    default boolean handlesAlerts() { return true; }
}
