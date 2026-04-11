package dev.demonz.redstonereboot.bukkit.managers;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Permission management with LuckPerms reflection hook and Bukkit fallback.
 */
public class PermissionManager {

    private final ConfigManager configManager;
    private Object luckPermsAPI;
    private boolean luckPermsAvailable;

    public PermissionManager(RedstoneRebootPlugin plugin) {
        this.configManager = plugin.getConfigManager();
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
                RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(luckPermsClass);
                if (provider != null) {
                    luckPermsAPI = provider.getProvider();
                    luckPermsAvailable = true;
                }
            } catch (Exception ignored) {
                luckPermsAPI = null;
                luckPermsAvailable = false;
            }
        }
    }

    public boolean hasPermission(Player player, String permission) {
        if (luckPermsAvailable && configManager.isLuckPermsIntegrationEnabled()) {
            try {
                Object userManager = luckPermsAPI.getClass().getMethod("getUserManager").invoke(luckPermsAPI);
                Object user = userManager.getClass()
                    .getMethod("getUser", java.util.UUID.class)
                    .invoke(userManager, player.getUniqueId());
                if (user != null) {
                    Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
                    Object permissionData = cachedData.getClass().getMethod("getPermissionData").invoke(cachedData);
                    Object result = permissionData.getClass()
                        .getMethod("checkPermission", String.class)
                        .invoke(permissionData, permission);
                    return (Boolean) result.getClass().getMethod("asBoolean").invoke(result);
                }
            } catch (Exception ignored) {
                // Fall through to Bukkit permissions.
            }
        }
        return player.hasPermission(permission);
    }

    public boolean isLuckPermsAvailable() {
        return luckPermsAvailable;
    }

    public boolean canRestartNow(Player player) {
        return hasPermission(player, "redstonereboot.restart.now") || hasAdminPermission(player);
    }

    public boolean canScheduleRestart(Player player) {
        return hasPermission(player, "redstonereboot.restart.schedule") || hasAdminPermission(player);
    }

    public boolean canCancelRestart(Player player) {
        return hasPermission(player, "redstonereboot.restart.cancel") || hasAdminPermission(player);
    }

    public boolean canViewStatus(Player player) {
        return hasPermission(player, "redstonereboot.status") || hasPermission(player, "redstonereboot.use");
    }

    public boolean canReloadConfig(Player player) {
        return hasPermission(player, "redstonereboot.config.reload") || hasAdminPermission(player);
    }

    public boolean hasAdminPermission(Player player) {
        return hasPermission(player, "redstonereboot.admin")
            || (configManager.isUseOpAsAdminEnabled() && player.isOp());
    }

    public boolean shouldReceiveNotifications(Player player) {
        return hasPermission(player, "redstonereboot.notify");
    }
}
