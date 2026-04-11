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
    
    private java.lang.reflect.Method getUserManagerMethod;
    private java.lang.reflect.Method getUserMethod;
    private java.lang.reflect.Method getCachedDataMethod;
    private java.lang.reflect.Method getPermissionDataMethod;
    private java.lang.reflect.Method checkPermissionMethod;
    private java.lang.reflect.Method asBooleanMethod;

    public PermissionManager(RedstoneRebootPlugin plugin) {
        this.configManager = plugin.getConfigManager();
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
                RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(luckPermsClass);
                if (provider != null) {
                    luckPermsAPI = provider.getProvider();
                    luckPermsAvailable = true;
                    prepareReflection();
                }
            } catch (Exception ignored) {
                luckPermsAPI = null;
                luckPermsAvailable = false;
            }
        }
    }

    private void prepareReflection() throws Exception {
        getUserManagerMethod = luckPermsAPI.getClass().getMethod("getUserManager");
        Class<?> userManagerClass = getUserManagerMethod.getReturnType();
        getUserMethod = userManagerClass.getMethod("getUser", java.util.UUID.class);
        Class<?> userClass = getUserMethod.getReturnType();
        getCachedDataMethod = userClass.getMethod("getCachedData");
        Class<?> cachedDataClass = getCachedDataMethod.getReturnType();
        getPermissionDataMethod = cachedDataClass.getMethod("getPermissionData");
        Class<?> permissionDataClass = getPermissionDataMethod.getReturnType();
        checkPermissionMethod = permissionDataClass.getMethod("checkPermission", String.class);
        Class<?> resultClass = checkPermissionMethod.getReturnType();
        asBooleanMethod = resultClass.getMethod("asBoolean");
    }

    public boolean hasPermission(Player player, String permission) {
        if (luckPermsAvailable && configManager.isLuckPermsIntegrationEnabled()) {
            try {
                Object userManager = getUserManagerMethod.invoke(luckPermsAPI);
                Object user = getUserMethod.invoke(userManager, player.getUniqueId());
                if (user != null) {
                    Object cachedData = getCachedDataMethod.invoke(user);
                    Object permissionData = getPermissionDataMethod.invoke(cachedData);
                    Object result = checkPermissionMethod.invoke(permissionData, permission);
                    return (Boolean) asBooleanMethod.invoke(result);
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
