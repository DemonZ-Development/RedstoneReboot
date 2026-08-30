package dev.demonz.redstonereboot.bukkit.managers;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import dev.demonz.redstonereboot.common.manager.RestartReason;
import dev.demonz.redstonereboot.common.text.LegacyTextUtil;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

public class AlertManager {

    private final RedstoneRebootPlugin plugin;
    private final ConfigManager configManager;
    private final PermissionManager permissionManager;
    private volatile boolean soundWarningLogged = false;

    public AlertManager(RedstoneRebootPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.permissionManager = plugin.getPermissionManager();
    }

    public void resetOnReload() {
        soundWarningLogged = false;
    }

    public void sendRestartAlert(int seconds, RestartReason reason) {
        if (!configManager.isAlertsEnabled()) {
            return;
        }

        String timeString = formatTime(seconds);
        List<Player> recipients = getNotificationRecipients();

        if (configManager.isChatAlertsEnabled()) {
            Component message = RedstoneRebootPlugin.LEGACY_SERIALIZER.deserialize(
                LegacyTextUtil.translateAlternateColorCodes(
                    configManager.getChatAlertFormat()
                        .replace("{time}", timeString)
                        .replace("{reason}", reason.getDisplayName())
                )
            );
            sendChat(recipients, message);
        }

        if (configManager.isTitleAlertsEnabled()) {
            Title title = Title.title(
                RedstoneRebootPlugin.LEGACY_SERIALIZER.deserialize(LegacyTextUtil.translateAlternateColorCodes(configManager.getTitleMainText())),
                RedstoneRebootPlugin.LEGACY_SERIALIZER.deserialize(LegacyTextUtil.translateAlternateColorCodes(configManager.getTitleSubText().replace("{time}", timeString))),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            );
            showTitle(recipients, title);
        }

        if (configManager.isActionBarAlertsEnabled()) {
            Component actionBar = RedstoneRebootPlugin.LEGACY_SERIALIZER.deserialize(
                LegacyTextUtil.translateAlternateColorCodes(
                    configManager.getActionBarFormat()
                        .replace("{time}", timeString)
                        .replace("{reason}", reason.getDisplayName())
                )
            );
            sendActionBar(recipients, actionBar);
        }

        playConfiguredSound(recipients);
        try {
            dev.demonz.redstonereboot.common.api.RedstoneRebootAPI api = dev.demonz.redstonereboot.common.api.RedstoneRebootAPI.getInstance();
            if (api != null) {
                String chatRaw = configManager.getChatAlertFormat().replace("{time}", timeString).replace("{reason}", reason.getDisplayName());
                api.dispatchMessage(new dev.demonz.redstonereboot.common.api.MessageContext(
                    dev.demonz.redstonereboot.common.api.MessageContext.Type.SCHEDULED_ALERT,
                    seconds, reason, "System", chatRaw, configManager.getTitleMainText(), configManager.getTitleSubText().replace("{time}", timeString),
                    System.currentTimeMillis(), plugin.getPlatformName(), plugin.getMinecraftVersion()));
            }
        } catch (Exception ignored) {}
    }

    public void sendFinalRestartAlert(RestartReason reason) {
        if (!configManager.isAlertsEnabled()) {
            return;
        }

        List<Player> recipients = getNotificationRecipients();

        Component message = RedstoneRebootPlugin.LEGACY_SERIALIZER.deserialize(
            LegacyTextUtil.translateAlternateColorCodes(configManager.getPrefix() + " &cServer is restarting NOW! Reason: &e" + reason.getDisplayName())
        );
        sendChat(recipients, message);
        if (configManager.isActionBarAlertsEnabled()) {
            sendActionBar(recipients, message);
        }
        playConfiguredSound(recipients);
        try {
            dev.demonz.redstonereboot.common.api.RedstoneRebootAPI api = dev.demonz.redstonereboot.common.api.RedstoneRebootAPI.getInstance();
            if (api != null) {
                String raw = configManager.getPrefix() + " &cServer is restarting NOW! Reason: &e" + reason.getDisplayName();
                api.dispatchMessage(new dev.demonz.redstonereboot.common.api.MessageContext(
                    dev.demonz.redstonereboot.common.api.MessageContext.Type.FINAL_ALERT,
                    0, reason, "System", raw, null, null,
                    System.currentTimeMillis(), plugin.getPlatformName(), plugin.getMinecraftVersion()));
            }
        } catch (Exception ignored) {}
    }

    public void sendRestartCancelledAlert() {
        if (!configManager.isAlertsEnabled()) {
            return;
        }

        List<Player> recipients = getNotificationRecipients();

        Component message = RedstoneRebootPlugin.LEGACY_SERIALIZER.deserialize(
            LegacyTextUtil.translateAlternateColorCodes(configManager.getPrefix() + " &aScheduled restart has been CANCELLED!")
        );
        sendChat(recipients, message);
        if (configManager.isActionBarAlertsEnabled()) {
            sendActionBar(recipients, message);
        }
        try {
            dev.demonz.redstonereboot.common.api.RedstoneRebootAPI api = dev.demonz.redstonereboot.common.api.RedstoneRebootAPI.getInstance();
            if (api != null) {
                String raw = configManager.getPrefix() + " &aScheduled restart has been CANCELLED!";
                api.dispatchMessage(new dev.demonz.redstonereboot.common.api.MessageContext(
                    dev.demonz.redstonereboot.common.api.MessageContext.Type.CANCELLED,
                    -1, null, "System", raw, null, null,
                    System.currentTimeMillis(), plugin.getPlatformName(), plugin.getMinecraftVersion()));
            }
        } catch (Exception ignored) {}
    }

    public void sendEmergencyAlert(String reason) {
        if (!configManager.isAlertsEnabled()) {
            return;
        }

        List<Player> recipients = getNotificationRecipients();

        Component message = RedstoneRebootPlugin.LEGACY_SERIALIZER.deserialize(
            LegacyTextUtil.translateAlternateColorCodes(configManager.getPrefix() + " &4&lEMERGENCY RESTART&r&c - " + reason)
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
        try {
            dev.demonz.redstonereboot.common.api.RedstoneRebootAPI api = dev.demonz.redstonereboot.common.api.RedstoneRebootAPI.getInstance();
            if (api != null) {
                String raw = configManager.getPrefix() + " &4&lEMERGENCY RESTART&r&c - " + reason;
                api.dispatchMessage(new dev.demonz.redstonereboot.common.api.MessageContext(
                    dev.demonz.redstonereboot.common.api.MessageContext.Type.EMERGENCY,
                    -1, dev.demonz.redstonereboot.common.manager.RestartReason.EMERGENCY_TPS, "EmergencyMonitor", raw, "&4&lEmergency Restart", "&c" + reason,
                    System.currentTimeMillis(), plugin.getPlatformName(), plugin.getMinecraftVersion()));
                api.fireEmergency(reason, dev.demonz.redstonereboot.common.manager.RestartReason.EMERGENCY_TPS);
            }
        } catch (Exception ignored) {}
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
            sendChat(recipients, RedstoneRebootPlugin.LEGACY_SERIALIZER.deserialize(LegacyTextUtil.translateAlternateColorCodes(message)));
        }

        if (configManager.isTitleAlertsEnabled()) {
            Title configuredTitle = Title.title(
                RedstoneRebootPlugin.LEGACY_SERIALIZER.deserialize(LegacyTextUtil.translateAlternateColorCodes(title)),
                RedstoneRebootPlugin.LEGACY_SERIALIZER.deserialize(LegacyTextUtil.translateAlternateColorCodes(subtitle)),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(1500), Duration.ofMillis(500))
            );
            showTitle(recipients, configuredTitle);
        }

        if (configManager.isActionBarAlertsEnabled()) {
            sendActionBar(recipients, RedstoneRebootPlugin.LEGACY_SERIALIZER.deserialize(LegacyTextUtil.translateAlternateColorCodes(subtitle)));
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
            if (!soundWarningLogged) {
                plugin.getLogger().warning("Invalid sound name '" + configManager.getSoundName()
                    + "' configured in config.yml. Sound alerts will not play.");
                soundWarningLogged = true;
            }
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
                : minutes + ":" + String.format(Locale.ROOT, "%02d", remainder);
        }
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        return minutes == 0
            ? hours + " hour" + (hours != 1 ? "s" : "")
            : hours + "h " + minutes + "m";
    }
}
