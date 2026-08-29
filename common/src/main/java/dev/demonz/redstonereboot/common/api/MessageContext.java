/*
 * Copyright (c) 2026 DemonZ Development
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package dev.demonz.redstonereboot.common.api;

import dev.demonz.redstonereboot.common.manager.RestartReason;

/** One alert from RedstoneReboot — passed to your MessageAdapter. Use toPlainText() for Discord. */
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

    /**
     * Plain-text representation suitable for Discord/external services.
     */
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
