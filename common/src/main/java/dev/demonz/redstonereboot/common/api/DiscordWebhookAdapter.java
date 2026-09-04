package dev.demonz.redstonereboot.common.api;

import dev.demonz.redstonereboot.common.RedstoneRebootCore;
import dev.demonz.redstonereboot.common.text.LegacyTextUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DiscordWebhookAdapter implements MessageAdapter {

    private final Logger logger;
    private final String webhookUrl;
    private final String username;
    private final HttpClient httpClient;

    public DiscordWebhookAdapter(Logger logger, String webhookUrl) {
        this(logger, webhookUrl, "RedstoneReboot");
    }

    public DiscordWebhookAdapter(Logger logger, String webhookUrl, String username) {
        this.logger = logger;
        this.webhookUrl = webhookUrl;
        this.username = username != null ? username : "RedstoneReboot";
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @Override
    public void onMessage(MessageContext context) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        if (context.getType() == MessageContext.Type.GENERIC_CHAT && context.getSecondsUntilRestart() > 0) {
            return;
        }
        String content = LegacyTextUtil.stripLegacyFormatting(context.toPlainText());
        if (content.isBlank()) return;

        String title;
        int color;
        switch (context.getType()) {
            case FINAL_ALERT -> { title = "\uD83D\uDD34 Server Restarting NOW"; color = 0xFF0000; }
            case SCHEDULED_ALERT -> { title = "\u23F0 Restart in " + formatSeconds(context.getSecondsUntilRestart()); color = 0xFFA500; }
            case CANCELLED -> { title = "\u2705 Restart Cancelled"; color = 0x00FF00; }
            case EMERGENCY -> { title = "\uD83D\uDEA8 Emergency Restart"; color = 0xFF0000; }
            case POSTPONED -> { title = "\u26A0\uFE0F Restart Postponed"; color = 0xFFFF00; }
            default -> { title = "RedstoneReboot"; color = 0x3498DB; }
        }

        String reason = context.getReason() != null ? context.getReason().getDisplayName() : "Unknown";
        String description = content;
        if (context.getReason() != null && context.getType() != MessageContext.Type.POSTPONED) {
            description += "\n**Reason:** " + reason;
        }
        if (context.getInitiator() != null) {
            description += "\n**Initiator:** " + context.getInitiator();
        }
        String platform = context.getPlatformName() != null ? context.getPlatformName() : "Unknown";
        description += "\n**Platform:** " + platform;

        String json = "{"
            + "\"username\":\"" + escapeJson(username) + "\","
            + "\"embeds\":[{"
            + "\"title\":\"" + escapeJson(title) + "\","
            + "\"description\":\"" + escapeJson(description) + "\","
            + "\"color\":" + color + ","
            + "\"timestamp\":\"" + java.time.Instant.ofEpochMilli(context.getTimestamp()).toString() + "\""
            + "}]"
            + "}";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(webhookUrl))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .header("User-Agent", "RedstoneReboot-DiscordWebhook/" + RedstoneRebootCore.VERSION)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        CompletableFuture.runAsync(() -> {
            try {
                HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                    logger.log(Level.WARNING, "Discord webhook failed: HTTP " + resp.statusCode() + " - " + resp.body());
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Discord webhook request failed", e);
            }
        });
    }

    @Override
    public String getName() {
        return "DiscordWebhook";
    }

    private static String formatSeconds(int seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
