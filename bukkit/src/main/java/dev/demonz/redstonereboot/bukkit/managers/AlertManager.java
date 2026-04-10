package dev.demonz.redstonereboot.bukkit.managers;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.time.Duration;

/**
 * Manages all player-facing alerts using Kyori Adventure for cross-version support.
 */
public class AlertManager {
    private final RedstoneRebootPlugin plugin;
    private final ConfigManager configManager;
    private final PermissionManager permissionManager;

    public AlertManager(RedstoneRebootPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.permissionManager = plugin.getPermissionManager();
    }

    public void sendRestartAlert(int seconds, RestartManager.RestartReason reason) {
        if (!configManager.isAlertsEnabled()) return;
        String timeString = formatTime(seconds);
        BukkitAudiences adv = plugin.getAdventure();
        if (adv == null) return;

        // Chat
        if (configManager.isChatAlertsEnabled()) {
            String message = configManager.getChatAlertFormat()
                .replace("{time}", timeString)
                .replace("{reason}", reason.getDisplayName());
            Component comp = LegacyComponentSerializer.legacySection().deserialize(message);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (permissionManager.shouldReceiveNotifications(p))
                    adv.player(p).sendMessage(comp);
            }
        }

        // Title
        if (configManager.isTitleAlertsEnabled()) {
            Component main = LegacyComponentSerializer.legacySection().deserialize(configManager.getTitleMainText());
            Component sub = LegacyComponentSerializer.legacySection().deserialize(
                configManager.getTitleSubText().replace("{time}", timeString));
            Title title = Title.title(main, sub,
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500)));
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (permissionManager.shouldReceiveNotifications(p))
                    adv.player(p).showTitle(title);
            }
        }

        // ActionBar
        if (configManager.isActionBarAlertsEnabled()) {
            String msg = configManager.getActionBarFormat()
                .replace("{time}", timeString)
                .replace("{reason}", reason.getDisplayName());
            Component comp = LegacyComponentSerializer.legacySection().deserialize(msg);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (permissionManager.shouldReceiveNotifications(p))
                    adv.player(p).sendActionBar(comp);
            }
        }

        // Sound
        if (configManager.isSoundAlertsEnabled()) {
            try {
                Sound sound = Sound.valueOf(configManager.getSoundName());
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (permissionManager.shouldReceiveNotifications(p))
                        p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void sendFinalRestartAlert(RestartManager.RestartReason reason) {
        BukkitAudiences adv = plugin.getAdventure();
        if (adv == null) return;
        Component comp = LegacyComponentSerializer.legacySection().deserialize(
            configManager.getPrefix() + " §cServer is restarting NOW! Reason: §e" + reason.getDisplayName());
        adv.all().sendMessage(comp);
    }

    public void sendRestartCancelledAlert() {
        BukkitAudiences adv = plugin.getAdventure();
        if (adv == null) return;
        Component comp = LegacyComponentSerializer.legacySection().deserialize(
            configManager.getPrefix() + " §aScheduled restart has been CANCELLED!");
        adv.all().sendMessage(comp);
    }

    public void sendEmergencyAlert(String reason) {
        BukkitAudiences adv = plugin.getAdventure();
        if (adv == null) return;
        Component comp = LegacyComponentSerializer.legacySection().deserialize(
            configManager.getPrefix() + " §4§lEMERGENCY RESTART§r§c — " + reason);
        adv.all().sendMessage(comp);
        try {
            for (Player p : Bukkit.getOnlinePlayers())
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
        } catch (Exception e) {
            for (Player p : Bukkit.getOnlinePlayers())
                p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.5f);
        }
    }

    private String formatTime(int seconds) {
        if (seconds < 60) return seconds + " second" + (seconds != 1 ? "s" : "");
        if (seconds < 3600) {
            int m = seconds / 60, s = seconds % 60;
            return s == 0 ? m + " minute" + (m != 1 ? "s" : "") : m + ":" + String.format("%02d", s);
        }
        int h = seconds / 3600, m = (seconds % 3600) / 60;
        return m == 0 ? h + " hour" + (h != 1 ? "s" : "") : h + "h " + m + "m";
    }
}
