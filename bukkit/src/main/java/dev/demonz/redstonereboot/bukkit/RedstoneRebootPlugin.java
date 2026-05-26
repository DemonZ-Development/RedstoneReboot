package dev.demonz.redstonereboot.bukkit;

import dev.demonz.redstonereboot.bukkit.commands.RebootCommand;
import dev.demonz.redstonereboot.bukkit.integrations.PlaceholderAPIHook;
import dev.demonz.redstonereboot.bukkit.listeners.ServerEventListener;
import dev.demonz.redstonereboot.bukkit.managers.AlertManager;
import dev.demonz.redstonereboot.bukkit.managers.ConfigManager;
import dev.demonz.redstonereboot.bukkit.managers.PermissionManager;
import dev.demonz.redstonereboot.bukkit.scheduler.BukkitSchedulerFactory;
import dev.demonz.redstonereboot.bukkit.utils.ServerLoadMonitor;
import dev.demonz.redstonereboot.common.RedstoneRebootCore;
import dev.demonz.redstonereboot.common.manager.RestartManager;
import dev.demonz.redstonereboot.common.manager.RestartReason;
import dev.demonz.redstonereboot.common.platform.ServerPlatform;
import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Main entry point for RedstoneReboot on Bukkit, Paper, and Folia.
 */
public class RedstoneRebootPlugin extends JavaPlugin implements ServerPlatform {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private static RedstoneRebootPlugin instance;

    private BukkitAudiences adventure;
    private volatile PlatformTaskScheduler taskScheduler;
    private RedstoneRebootCore core;
    private ConfigManager configManager;
    private AlertManager alertManager;
    private PermissionManager permissionManager;
    private ServerLoadMonitor serverLoadMonitor;
    private PlaceholderAPIHook placeholderHook;

    @Override
    public void onEnable() {
        instance = this;

        try {
            taskScheduler = BukkitSchedulerFactory.create(this);
            adventure = BukkitAudiences.create(this);
            configManager = new ConfigManager(this);
            
            // Initialize Core first, which handles internal managers
            core = new RedstoneRebootCore(this, taskScheduler, configManager, getDataFolder().toPath());
            
            permissionManager = new PermissionManager(this);
            alertManager = new AlertManager(this);

            registerCommand();
            getServer().getPluginManager().registerEvents(new ServerEventListener(this), this);

            core.onEnable();
            restartMonitoring();
            hookPlaceholderAPI();
            initializeMetrics();
            logIntegrationStatus();
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Failed to enable RedstoneReboot.", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
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
        try {
            configManager.reloadConfig();
            stopMonitoring();
            unhookPlaceholderAPI();

            core.onDisable();
            core.onEnable();

            restartMonitoring();
            hookPlaceholderAPI();
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Reload failed — attempting to restore previous state.", exception);
            if (core != null) {
                try { core.onDisable(); } catch (Exception e) { getLogger().log(Level.WARNING, "Recovery: core.onDisable() failed", e); }
                try { core.onEnable(); } catch (Exception e) { getLogger().log(Level.WARNING, "Recovery: core.onEnable() failed", e); }
            }
            try { restartMonitoring(); } catch (Exception e) { getLogger().log(Level.WARNING, "Recovery: restartMonitoring() failed", e); }
            try { hookPlaceholderAPI(); } catch (Exception e) { getLogger().log(Level.WARNING, "Recovery: hookPlaceholderAPI() failed", e); }
            throw exception;
        }
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
    public void sendAlert(String message, String title, String subtitle) {
        if (alertManager != null) {
            alertManager.sendAlert(message, title, subtitle);
        } else {
            broadcastMessage(message);
            broadcastTitle(title, subtitle);
        }
    }

    @Override
    public void sendRestartAlert(int seconds, RestartReason reason) {
        if (alertManager != null) {
            alertManager.sendRestartAlert(seconds, reason);
        } else {
            ServerPlatform.super.sendRestartAlert(seconds, reason);
        }
    }

    @Override
    public void sendFinalRestartAlert(RestartReason reason) {
        if (alertManager != null) {
            alertManager.sendFinalRestartAlert(reason);
        } else {
            ServerPlatform.super.sendFinalRestartAlert(reason);
        }
    }

    @Override
    public void sendRestartCancelledAlert() {
        if (alertManager != null) {
            alertManager.sendRestartCancelledAlert();
        } else {
            ServerPlatform.super.sendRestartCancelledAlert();
        }
    }

    @Override
    public void sendEmergencyAlert(String reason) {
        if (alertManager != null) {
            alertManager.sendEmergencyAlert(reason);
        } else {
            ServerPlatform.super.sendEmergencyAlert(reason);
        }
    }

    @Override
    public void executeConsole(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private static java.lang.reflect.Method cachedTpsMethod;
    private static java.lang.reflect.Method cachedGetServerMethod;
    private static java.lang.reflect.Field cachedRecentTpsField;

    @Override
    public double getTPS() {
        if (cachedTpsMethod == null) {
            try {
                cachedTpsMethod = Bukkit.class.getMethod("getTPS");
            } catch (Exception ignored) {}
        }
        if (cachedTpsMethod != null) {
            try {
                double[] tps = (double[]) cachedTpsMethod.invoke(null);
                if (tps.length > 0) return Math.min(tps[0], 20.0D);
            } catch (Exception ignored) {}
        }

        if (cachedGetServerMethod == null) {
            try {
                cachedGetServerMethod = Bukkit.getServer().getClass().getMethod("getServer");
            } catch (Exception ignored) {}
        }
        if (cachedGetServerMethod != null && cachedRecentTpsField == null) {
            try {
                Object nmsServer = cachedGetServerMethod.invoke(Bukkit.getServer());
                cachedRecentTpsField = nmsServer.getClass().getField("recentTps");
            } catch (Exception ignored) {}
        }
        if (cachedRecentTpsField != null) {
            try {
                Object nmsServer = cachedGetServerMethod.invoke(Bukkit.getServer());
                double[] recentTps = (double[]) cachedRecentTpsField.get(nmsServer);
                if (recentTps.length > 0) return Math.min(recentTps[0], 20.0D);
            } catch (Exception ignored) {}
        }

        return 20.0D;
    }

    @Override
    public String getPlatformName() {
        return BukkitSchedulerFactory.isFoliaEnvironment() ? "Folia" : Bukkit.getName();
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
        taskScheduler.runLater(() -> Bukkit.getServer().shutdown(), (long) configManager.getShutdownDelayTicks());
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
        stopMonitoring();
        if (!configManager.isMonitoringEnabled() && !configManager.isEmergencyRestartEnabled()) {
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

    private void initializeMetrics() {
        if (!configManager.getRawConfig().getBoolean("advanced.metrics-enabled", true)) {
            return;
        }

        try {
            Metrics metrics = new Metrics(this, 30751);

            metrics.addCustomChart(new SimplePie("active_backend",
                () -> core != null ? core.getBackendRegistry().getActiveBackend().getName() : "Unknown"));

            metrics.addCustomChart(new SimplePie("scheduled_restarts",
                () -> configManager.isScheduledRestartsEnabled() ? "Enabled" : "Disabled"));

            metrics.addCustomChart(new SimplePie("monitoring_enabled",
                () -> configManager.isMonitoringEnabled() ? "Enabled" : "Disabled"));

            metrics.addCustomChart(new SimplePie("platform_type",
                () -> taskScheduler.isFolia() ? "Folia" : "Bukkit"));

            getLogger().info("bStats metrics initialized (ID: 30751).");
        } catch (Exception exception) {
            getLogger().fine("bStats initialization skipped: " + exception.getMessage());
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
        getLogger().info("bStats        : " + (configManager.getRawConfig().getBoolean("advanced.metrics-enabled", true) ? "ENABLED (ID: 30751)" : "DISABLED"));
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
        return core != null ? core.getRestartManager() : null;
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
