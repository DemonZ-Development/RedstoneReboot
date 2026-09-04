package dev.demonz.redstonereboot.common.api;

import dev.demonz.redstonereboot.common.manager.RestartReason;

public final class MessageContext {

    public enum Type {
        SCHEDULED_ALERT,
        FINAL_ALERT,
        CANCELLED,
        EMERGENCY,
        POSTPONED,
        GENERIC_CHAT,
        TITLE,
        ACTION_BAR
    }

    private final Type type;
    private final int secondsUntilRestart;
    private final RestartReason reason;
    private final String initiator;
    private final String chatMessage;
    private final String title;
    private final String subtitle;
    private final long timestamp;
    private final String platformName;
    private final String minecraftVersion;

    public MessageContext(Type type, int secondsUntilRestart, RestartReason reason, String initiator,
                          String chatMessage, String title, String subtitle,
                          long timestamp, String platformName, String minecraftVersion) {
        this.type = type;
        this.secondsUntilRestart = secondsUntilRestart;
        this.reason = reason;
        this.initiator = initiator;
        this.chatMessage = chatMessage;
        this.title = title;
        this.subtitle = subtitle;
        this.timestamp = timestamp;
        this.platformName = platformName;
        this.minecraftVersion = minecraftVersion;
    }

    public Type getType() { return type; }
    public int getSecondsUntilRestart() { return secondsUntilRestart; }
    public RestartReason getReason() { return reason; }
    public String getInitiator() { return initiator; }
    public String getChatMessage() { return chatMessage; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public long getTimestamp() { return timestamp; }
    public String getPlatformName() { return platformName; }
    public String getMinecraftVersion() { return minecraftVersion; }

    public String toPlainText() {
        if (chatMessage != null && !chatMessage.isBlank()) {
            return chatMessage;
        }
        if (title != null && !title.isBlank()) {
            return title + (subtitle != null && !subtitle.isBlank() ? " - " + subtitle : "");
        }
        return type.name() + " (" + (reason != null ? reason.getDisplayName() : "unknown") + ")";
    }
}
