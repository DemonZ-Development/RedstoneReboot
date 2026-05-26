package dev.demonz.redstonereboot.bukkit.listeners;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import dev.demonz.redstonereboot.common.manager.RestartReason;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ServerEventListener implements Listener {

    private final RedstoneRebootPlugin plugin;

    public ServerEventListener(RedstoneRebootPlugin plugin) {
        this.plugin = plugin;
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getPermissionManager() == null || !plugin.getPermissionManager().hasAdminPermission(event.getPlayer())) {
            return;
        }

        dev.demonz.redstonereboot.common.manager.RestartManager restartManager = plugin.getRestartManager();
        if (restartManager != null) {
            if (restartManager.isRestartInProgress()) {
                event.getPlayer().sendMessage(plugin.getConfigManager().getPrefix()
                    + " §eRestart in progress - §c"
                    + (restartManager.getCurrentRestartReason() != null ? restartManager.getCurrentRestartReason().getDisplayName() : "Unknown"));
            } else {
                java.time.ZonedDateTime nextRestart = restartManager.getNextScheduledRestart();
                if (nextRestart != null) {
                    event.getPlayer().sendMessage(plugin.getConfigManager().getPrefix()
                        + " §aNext restart: §e"
                        + nextRestart.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        + " " + plugin.getConfigManager().getTimezone());
                }
            }
        }

        dev.demonz.redstonereboot.common.RedstoneRebootCore core = plugin.getCore();
        if (core != null && core.getUpdateChecker() != null && core.getUpdateChecker().hasUpdate()) {
            event.getPlayer().sendMessage(plugin.getConfigManager().getPrefix()
                + " §aA new update for RedstoneReboot is available on Modrinth! Latest: v"
                + core.getUpdateChecker().getLatestVersion());
        }
    }
}
