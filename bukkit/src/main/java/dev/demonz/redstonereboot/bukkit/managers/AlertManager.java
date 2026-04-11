package dev.demonz.redstonereboot.bukkit.managers;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import dev.demonz.redstonereboot.common.manager.RestartReason;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;

/**
 * Manages all player-facing alerts using Kyori Adventure for cross-version support.
 */
public class AlertManager {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private final RedstoneRebootPlugin plugin;
    private final ConfigManager configManager;
    private final PermissionManager permissionManager;

    public AlertManager(RedstoneRebootPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.permissionManager = plugin.getPermissionManager();
    }

    public void sendRestartAlert(int seconds, RestartReason reason) {
        if (!configManager.isAlertsEnabled()) {
            return;
        }

        String timeString = formatTime(seconds);
        List<Player> recipients = getNotificationRecipients();
        if (recipients.isEmpty()) {
            return;
        }

        if (configManager.isChatAlertsEnabled()) {
            Component message = LEGACY_SERIALIZER.deserialize(
                configManager.getChatAlertFormat()
                    .replace("{time}", timeString)
                    .replace("{reason}", reason.getDisplayName())
            );
            sendChat(recipients, message);
        }

        if (configManager.isTitleAlertsEnabled()) {
            Title title = Title.title(
                LEGACY_SERIALIZER.deserialize(configManager.getTitleMainText()),
                LEGACY_SERIALIZER.deserialize(configManager.getTitleSubText().replace("{time}", timeString)),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            );
            showTitle(recipients, title);
        }

        if (configManager.isActionBarAlertsEnabled()) {
            Component actionBar = LEGACY_SERIALIZER.deserialize(
                configManager.getActionBarFormat()
                    .replace("{time}", timeString)
                    .replace("{reason}", reason.getDisplayName())
            );
            sendActionBar(recipients, actionBar);
        }

        playConfiguredSound(recipients);
    }

    public void sendFinalRestartAlert(RestartReason reason) {
        if (!configManager.isAlertsEnabled()) {
            return;
        }

        List<Player> recipients = getNotificationRecipients();
        if (recipients.isEmpty()) {
            return;
        }

        Component message = LEGACY_SERIALIZER.deserialize(
            configManager.getPrefix() + " §cServer is restarting NOW! Reason: §e" + reason.getDisplayName()
        );
        sendChat(recipients, message);
        if (configManager.isActionBarAlertsEnabled()) {
            sendActionBar(recipients, message);
        }
        playConfiguredSound(recipients);
    }

    public void sendRestartCancelledAlert() {
        if (!configManager.isAlertsEnabled()) {
            return;
        }

        List<Player> recipients = getNotificationRecipients();
        if (recipients.isEmpty()) {
            return;
        }

        Component message = LEGACY_SERIALIZER.deserialize(
            configManager.getPrefix() + " §aScheduled restart has been CANCELLED!"
        );
        sendChat(recipients, message);
        if (configManager.isActionBarAlertsEnabled()) {
            sendActionBar(recipients, message);
        }
    }

    public void sendEmergencyAlert(String reason) {
        if (!configManager.isAlertsEnabled()) {
            return;
        }

        List<Player> recipients = getNotificationRecipients();
        if (recipients.isEmpty()) {
            return;
        }

        Component message = LEGACY_SERIALIZER.deserialize(
            configManager.getPrefix() + " §4§lEMERGENCY RESTART§r§c - " + reason
        );
        sendChat(recipients, message);
        if (configManager.isActionBarAlertsEnabled()) {
            sendActionBar(recipients, message);
        }

        for (Player player : recipients) {
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
            } catch (Exception exception) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.5f);
            }
        }
    }

    public void sendAlert(String message, String title, String subtitle) {
        if (!configManager.isAlertsEnabled()) {
            return;
        }

        List<Player> recipients = getNotificationRecipients();
        if (recipients.isEmpty()) {
            return;
        }

        if (configManager.isChatAlertsEnabled()) {
            sendChat(recipients, LEGACY_SERIALIZER.deserialize(message));
        }

        if (configManager.isTitleAlertsEnabled()) {
            Title configuredTitle = Title.title(
                LEGACY_SERIALIZER.deserialize(title),
                LEGACY_SERIALIZER.deserialize(subtitle),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(1500), Duration.ofMillis(500))
            );
            showTitle(recipients, configuredTitle);
        }

        if (configManager.isActionBarAlertsEnabled()) {
            sendActionBar(recipients, LEGACY_SERIALIZER.deserialize(subtitle));
        }

        playConfiguredSound(recipients);
    }

    private List<Player> getNotificationRecipients() {
        return new java.util.ArrayList<Player>(Bukkit.getOnlinePlayers()).stream()
            .filter(permissionManager::shouldReceiveNotifications)
            .toList();
    }

    private void sendChat(List<Player> recipients, Component message) {
        BukkitAudiences adv = plugin.getAdventure();
        if (adv == null) {
            return;
        }

        for (Player player : recipients) {
            adv.player(player).sendMessage(message);
        }
    }

    private void showTitle(List<Player> recipients, Title title) {
        BukkitAudiences adv = plugin.getAdventure();
        if (adv == null) {
            return;
        }

        for (Player player : recipients) {
            adv.player(player).showTitle(title);
        }
    }

    private void sendActionBar(List<Player> recipients, Component message) {
        BukkitAudiences adv = plugin.getAdventure();
        if (adv == null) {
            return;
        }

        for (Player player : recipients) {
            adv.player(player).sendActionBar(message);
        }
    }

    private void playConfiguredSound(List<Player> recipients) {
        if (!configManager.isSoundAlertsEnabled()) {
            return;
        }

        try {
            Sound sound = Sound.valueOf(configManager.getSoundName());
            for (Player player : recipients) {
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    private String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + " second" + (seconds != 1 ? "s" : "");
        }
        if (seconds < 3600) {
            int minutes = seconds / 60;
            int remainder = seconds % 60;
            return remainder == 0
                ? minutes + " minute" + (minutes != 1 ? "s" : "")
                : minutes + ":" + String.format("%02d", remainder);
        }
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        return minutes == 0
            ? hours + " hour" + (hours != 1 ? "s" : "")
            : hours + "h " + minutes + "m";
    }
}
