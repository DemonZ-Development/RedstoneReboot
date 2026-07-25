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


package dev.demonz.redstonereboot.bukkit.listeners;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import dev.demonz.redstonereboot.common.RedstoneRebootCore;
import dev.demonz.redstonereboot.common.manager.RestartManager;
import dev.demonz.redstonereboot.common.manager.RestartReason;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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

        RestartManager restartManager = plugin.getRestartManager();
        if (restartManager != null) {
            if (restartManager.isRestartInProgress()) {
                event.getPlayer().sendMessage(plugin.getConfigManager().getPrefix()
                    + " §eRestart in progress - §c"
                    + (restartManager.getCurrentRestartReason() != null ? restartManager.getCurrentRestartReason().getDisplayName() : "Unknown"));
            } else {
                ZonedDateTime nextRestart = restartManager.getNextScheduledRestart();
                if (nextRestart != null) {
                    event.getPlayer().sendMessage(plugin.getConfigManager().getPrefix()
                        + " §aNext restart: §e"
                        + nextRestart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withLocale(Locale.ROOT))
                        + " " + plugin.getConfigManager().getTimezone());
                }
            }
        }

        RedstoneRebootCore core = plugin.getCore();
        if (core != null && core.getUpdateChecker() != null && core.getUpdateChecker().hasUpdate()) {
            event.getPlayer().sendMessage(plugin.getConfigManager().getPrefix()
                + " §aA new update for RedstoneReboot is available on Modrinth! Latest: v"
                + core.getUpdateChecker().getLatestVersion());
        }
    }
}