package dev.demonz.redstonereboot.common.scheduler;

public interface PlatformTaskScheduler {

    ScheduledTaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks);

    ScheduledTaskHandle runLater(Runnable task, long delayTicks);

    boolean isFolia();
}
