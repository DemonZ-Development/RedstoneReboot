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
        if (!plugin.getPermissionManager().hasAdminPermission(event.getPlayer())) {
            return;
        }

        if (plugin.getRestartManager().isRestartInProgress()) {
            event.getPlayer().sendMessage(plugin.getConfigManager().getPrefix()
                + " §eRestart in progress - §c"
                + plugin.getRestartManager().getCurrentRestartReason().getDisplayName());
        } else if (plugin.getRestartManager().getNextScheduledRestart() != null) {
            event.getPlayer().sendMessage(plugin.getConfigManager().getPrefix()
                + " §aNext restart: §e"
                + plugin.getRestartManager().getNextScheduledRestart().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                + " " + plugin.getConfigManager().getTimezone());
        }

        if (plugin.getCore().getUpdateChecker().hasUpdate()) {
            event.getPlayer().sendMessage(plugin.getConfigManager().getPrefix()
                + " §aA new update for RedstoneReboot is available on Modrinth! Latest: v"
                + plugin.getCore().getUpdateChecker().getLatestVersion());
        }
    }
}
