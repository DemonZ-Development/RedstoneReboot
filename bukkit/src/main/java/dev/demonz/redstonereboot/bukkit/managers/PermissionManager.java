package dev.demonz.redstonereboot.bukkit.managers;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Permission management with LuckPerms reflection hook and Bukkit fallback.
 */
public class PermissionManager {
    private final RedstoneRebootPlugin plugin;
    private final ConfigManager configManager;
    private Object luckPermsAPI = null;
    private boolean luckPermsAvailable = false;

    public PermissionManager(RedstoneRebootPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                Class<?> lpClass = Class.forName("net.luckperms.api.LuckPerms");
                RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(lpClass);
                if (provider != null) { luckPermsAPI = provider.getProvider(); luckPermsAvailable = true; }
            } catch (Exception ignored) {}
        }
    }

    public boolean hasPermission(Player player, String permission) {
        if (luckPermsAvailable && configManager.isLuckPermsIntegrationEnabled()) {
            try {
                Object um = luckPermsAPI.getClass().getMethod("getUserManager").invoke(luckPermsAPI);
                Object user = um.getClass().getMethod("getUser", java.util.UUID.class).invoke(um, player.getUniqueId());
                if (user != null) {
                    Object cd = user.getClass().getMethod("getCachedData").invoke(user);
                    Object pd = cd.getClass().getMethod("getPermissionData").invoke(cd);
                    Object result = pd.getClass().getMethod("checkPermission", String.class).invoke(pd, permission);
                    return (Boolean) result.getClass().getMethod("asBoolean").invoke(result);
                }
            } catch (Exception ignored) {}
        }
        return player.hasPermission(permission);
    }

    public boolean isLuckPermsAvailable() { return luckPermsAvailable; }
    public boolean canRestartNow(Player p) { return hasPermission(p, "redstonereboot.restart.now") || hasPermission(p, "redstonereboot.admin"); }
    public boolean canScheduleRestart(Player p) { return hasPermission(p, "redstonereboot.restart.schedule") || hasPermission(p, "redstonereboot.admin"); }
    public boolean canCancelRestart(Player p) { return hasPermission(p, "redstonereboot.restart.cancel") || hasPermission(p, "redstonereboot.admin"); }
    public boolean canViewStatus(Player p) { return hasPermission(p, "redstonereboot.status") || hasPermission(p, "redstonereboot.use"); }
    public boolean canReloadConfig(Player p) { return hasPermission(p, "redstonereboot.config.reload") || hasPermission(p, "redstonereboot.admin"); }
    public boolean hasAdminPermission(Player p) { return hasPermission(p, "redstonereboot.admin") || p.isOp(); }
    public boolean shouldReceiveNotifications(Player p) { return hasPermission(p, "redstonereboot.notify"); }
}
