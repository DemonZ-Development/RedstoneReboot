package dev.demonz.redstonereboot.bukkit;

import dev.demonz.redstonereboot.bukkit.commands.RebootCommand;
import dev.demonz.redstonereboot.bukkit.integrations.PlaceholderAPIHook;
import dev.demonz.redstonereboot.bukkit.listeners.ServerEventListener;
import dev.demonz.redstonereboot.bukkit.managers.AlertManager;
import dev.demonz.redstonereboot.bukkit.managers.ConfigManager;
import dev.demonz.redstonereboot.bukkit.managers.PermissionManager;
import dev.demonz.redstonereboot.bukkit.managers.RestartManager;
import dev.demonz.redstonereboot.bukkit.scheduler.PlatformTaskScheduler;
import dev.demonz.redstonereboot.bukkit.utils.ServerLoadMonitor;
import dev.demonz.redstonereboot.common.RedstoneRebootCore;
import dev.demonz.redstonereboot.common.platform.ServerPlatform;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main entry point for RedstoneReboot on Bukkit, Paper, and Folia.
 */
public class RedstoneRebootPlugin extends JavaPlugin implements ServerPlatform {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private static RedstoneRebootPlugin instance;

    private BukkitAudiences adventure;
    private PlatformTaskScheduler taskScheduler;
    private RedstoneRebootCore core;
    private ConfigManager configManager;
    private RestartManager restartManager;
    private AlertManager alertManager;
    private PermissionManager permissionManager;
    private ServerLoadMonitor serverLoadMonitor;
    private PlaceholderAPIHook placeholderHook;

    @Override
    public void onEnable() {
        instance = this;

        try {
            taskScheduler = PlatformTaskScheduler.create(this);
            adventure = BukkitAudiences.create(this);
            core = new RedstoneRebootCore(this);
            configManager = new ConfigManager(this);
            permissionManager = new PermissionManager(this);
            alertManager = new AlertManager(this);
            restartManager = new RestartManager(this);

            registerCommand();
            getServer().getPluginManager().registerEvents(new ServerEventListener(this), this);

            restartManager.initialize();
            restartMonitoring();
            hookPlaceholderAPI();
            core.onEnable();
            logIntegrationStatus();
        } catch (Exception exception) {
            getLogger().severe("Failed to enable RedstoneReboot: " + exception.getMessage());
            exception.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (restartManager != null) {
                restartManager.cleanup();
            }
            stopMonitoring();
            unhookPlaceholderAPI();
            if (core != null) {
                core.onDisable();
            }
            if (adventure != null) {
                adventure.close();
                adventure = null;
            }
        } catch (Exception exception) {
            getLogger().severe("Error during shutdown: " + exception.getMessage());
        } finally {
            instance = null;
        }
    }

    public void reloadPluginState() {
        configManager.reloadConfig();
        restartManager.cleanup();
        stopMonitoring();
        unhookPlaceholderAPI();

        restartManager.initialize();
        restartMonitoring();
        hookPlaceholderAPI();
    }

    @Override
    public void broadcastMessage(String message) {
        if (adventure != null) {
            adventure.all().sendMessage(LEGACY_SERIALIZER.deserialize(message));
        }
    }

    @Override
    public void broadcastTitle(String title, String subtitle) {
        if (adventure != null) {
            adventure.all().showTitle(Title.title(
                LEGACY_SERIALIZER.deserialize(title),
                LEGACY_SERIALIZER.deserialize(subtitle)
            ));
        }
    }

    @Override
    public void executeConsole(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    @Override
    public double getTPS() {
        try {
            java.lang.reflect.Method tpsMethod = Bukkit.class.getMethod("getTPS");
            double[] tps = (double[]) tpsMethod.invoke(null);
            return tps.length > 0 ? Math.min(tps[0], 20.0D) : 20.0D;
        } catch (Exception ignored) {
            try {
                Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
                double[] recentTps = (double[]) server.getClass().getField("recentTps").get(server);
                return recentTps.length > 0 ? Math.min(recentTps[0], 20.0D) : 20.0D;
            } catch (Exception fallbackIgnored) {
                return 20.0D;
            }
        }
    }

    @Override
    public String getPlatformName() {
        String brand = PlatformTaskScheduler.isFoliaEnvironment() ? "Folia" : Bukkit.getName();
        return brand + " (Scheduler Adapter)";
    }

    @Override
    public String getMinecraftVersion() {
        return Bukkit.getBukkitVersion().split("-")[0];
    }

    @Override
    public int getOnlinePlayerCount() {
        return Bukkit.getOnlinePlayers().size();
    }

    @Override
    public void shutdownServer() {
        taskScheduler.runLater(() -> Bukkit.getServer().shutdown(), 60L);
    }

    private void registerCommand() {
        PluginCommand rebootCommand = getCommand("reboot");
        if (rebootCommand == null) {
            throw new IllegalStateException("Command 'reboot' is missing from plugin.yml");
        }

        RebootCommand handler = new RebootCommand(this);
        rebootCommand.setExecutor(handler);
        rebootCommand.setTabCompleter(handler);
    }

    private void restartMonitoring() {
        if (!configManager.isMonitoringEnabled()) {
            serverLoadMonitor = null;
            return;
        }

        serverLoadMonitor = new ServerLoadMonitor(this);
        serverLoadMonitor.startMonitoring();
    }

    private void stopMonitoring() {
        if (serverLoadMonitor != null) {
            serverLoadMonitor.stopMonitoring();
            serverLoadMonitor = null;
        }
    }

    private void hookPlaceholderAPI() {
        if (placeholderHook != null) {
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && configManager.isPlaceholderAPIEnabled()) {
            try {
                placeholderHook = new PlaceholderAPIHook(this);
                placeholderHook.register();
            } catch (Exception exception) {
                getLogger().warning("PlaceholderAPI hook failed: " + exception.getMessage());
                placeholderHook = null;
            }
        }
    }

    private void unhookPlaceholderAPI() {
        if (placeholderHook != null) {
            placeholderHook.unregister();
            placeholderHook = null;
        }
    }

    private void logIntegrationStatus() {
        getLogger().info("==========================================");
        getLogger().info("Scheduling    : " + (configManager.isScheduledRestartsEnabled() ? "ENABLED" : "DISABLED"));
        getLogger().info("Monitoring    : " + (configManager.isMonitoringEnabled() ? "ENABLED" : "DISABLED"));
        getLogger().info("Emergency     : " + (configManager.isEmergencyRestartEnabled() ? "ENABLED" : "DISABLED"));
        getLogger().info("Scheduler     : " + (taskScheduler.isFolia() ? "FOLIA GLOBAL" : "BUKKIT"));
        getLogger().info("LuckPerms     : " + (permissionManager.isLuckPermsAvailable() ? "HOOKED" : "NOT FOUND"));
        getLogger().info("PlaceholderAPI: " + (placeholderHook != null ? "HOOKED" : "NOT FOUND"));
        getLogger().info("Timezone      : " + configManager.getTimezone());
        getLogger().info("==========================================");
    }

    public static RedstoneRebootPlugin getInstance() {
        return instance;
    }

    public BukkitAudiences getAdventure() {
        return adventure;
    }

    public PlatformTaskScheduler getTaskScheduler() {
        return taskScheduler;
    }

    public RedstoneRebootCore getCore() {
        return core;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RestartManager getRestartManager() {
        return restartManager;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public ServerLoadMonitor getServerLoadMonitor() {
        return serverLoadMonitor;
    }
}
