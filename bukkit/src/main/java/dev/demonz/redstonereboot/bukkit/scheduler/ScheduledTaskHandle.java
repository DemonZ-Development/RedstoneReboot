package dev.demonz.redstonereboot.bukkit.scheduler;

@FunctionalInterface
public interface ScheduledTaskHandle {

    void cancel();
}
