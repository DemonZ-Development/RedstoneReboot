package dev.demonz.redstonereboot.common.scheduler;

@FunctionalInterface
public interface ScheduledTaskHandle {
    void cancel();
}
