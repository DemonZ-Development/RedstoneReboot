package dev.demonz.redstonereboot.common.api;

import dev.demonz.redstonereboot.common.manager.RestartReason;

public interface RestartListener {

    default void onRestartScheduled(int seconds, RestartReason reason, String initiator) {}

    default void onRestartCancelled(String previousReason, String previousInitiator) {}

    default void onRestartExecuting(RestartReason reason, String initiator) {}

    default void onRestartFailed(String detail) {}

    default void onLockoutStarted(int durationSeconds) {}

    default void onEmergencyTriggered(String emergencyReason, RestartReason restartReason) {}
}
