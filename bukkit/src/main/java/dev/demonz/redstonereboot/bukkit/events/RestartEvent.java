package dev.demonz.redstonereboot.bukkit.events;

import dev.demonz.redstonereboot.bukkit.managers.RestartManager;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a server restart is initiated.
 * <p>Other plugins can listen for this event and optionally cancel it.</p>
 */
public class RestartEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final RestartManager.RestartReason reason;
    private final String initiator;
    private final int delaySeconds;
    private boolean cancelled = false;
    private String cancellationReason = "";

    public RestartEvent(RestartManager.RestartReason reason, String initiator, int delaySeconds) {
        this.reason = reason;
        this.initiator = initiator;
        this.delaySeconds = delaySeconds;
    }

    public RestartManager.RestartReason getReason() { return reason; }
    public String getInitiator() { return initiator; }
    public int getDelaySeconds() { return delaySeconds; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    public void setCancelled(boolean cancel, String reason) {
        this.cancelled = cancel;
        this.cancellationReason = reason != null ? reason : "";
    }

    public String getCancellationReason() { return cancellationReason; }

    @Override public @NotNull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
