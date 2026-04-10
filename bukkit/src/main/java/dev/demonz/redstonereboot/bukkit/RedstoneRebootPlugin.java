package dev.demonz.redstonereboot.bukkit;

import dev.demonz.redstonereboot.bukkit.commands.RebootCommand;
import dev.demonz.redstonereboot.bukkit.events.RestartEvent;
import dev.demonz.redstonereboot.bukkit.integrations.PlaceholderAPIHook;
import dev.demonz.redstonereboot.bukkit.listeners.ServerEventListener;
import dev.demonz.redstonereboot.bukkit.managers.*;
import dev.demonz.redstonereboot.bukkit.utils.ServerLoadMonitor;
import dev.demonz.redstonereboot.common.RedstoneRebootCore;
import dev.demonz.redstonereboot.common.platform.ServerPlatform;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main entry point for RedstoneReboot on Bukkit/Spigot/Paper.
 * <p>
 * Supports Minecraft 1.9 through 1.21.1 via adventure-platform-bukkit
 * (cross-version title/chat/actionbar/sound adapter).
 * </p>
 *
 * @author DemonZ Development
 * @since 1.0.0
 */
public class RedstoneRebootPlugin extends JavaPlugin implements ServerPlatform {

    private static RedstoneRebootPlugin instance;

    private BukkitAudiences adventure;
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
            // Initialize Kyori Adventure (cross-version messaging 1.9-1.21.1)
            this.adventure = BukkitAudiences.create(this);

            // Initialize core engine
            this.core = new RedstoneRebootCore(this);

            // Initialize managers
            this.configManager = new ConfigManager(this);
            this.permissionManager = new PermissionManager(this);
            this.alertManager = new AlertManager(this);
            this.restartManager = new RestartManager(this);

            // Initialize monitoring
            if (configManager.isMonitoringEnabled()) {
                this.serverLoadMonitor = new ServerLoadMonitor(this);
                serverLoadMonitor.startMonitoring();
            }

            // Register commands
            RebootCommand rebootCommand = new RebootCommand(this);
            getCommand("reboot").setExecutor(rebootCommand);
            getCommand("reboot").setTabCompleter(rebootCommand);

            // Register events
            getServer().getPluginManager().registerEvents(new ServerEventListener(this), this);

            // Initialize restart scheduling
            restartManager.initialize();

            // Hook PlaceholderAPI
            hookPlaceholderAPI();

            // Print startup via core engine
            core.onEnable();

            // Log integration status
            getLogger().info("┌─────────────────────────────────────┐");
            getLogger().info("│  Integration Status                 │");
            getLogger().info("├─────────────────────────────────────┤");
            getLogger().info("│  Scheduling:     " + pad(configManager.isScheduledRestartsEnabled() ? "✓ ENABLED" : "✗ DISABLED"));
            getLogger().info("│  Monitoring:     " + pad(configManager.isMonitoringEnabled() ? "✓ ENABLED" : "✗ DISABLED"));
            getLogger().info("│  Emergency:      " + pad(configManager.isEmergencyRestartEnabled() ? "✓ ENABLED" : "✗ DISABLED"));
            getLogger().info("│  LuckPerms:      " + pad(permissionManager.isLuckPermsAvailable() ? "✓ HOOKED" : "— NOT FOUND"));
            getLogger().info("│  PlaceholderAPI: " + pad(placeholderHook != null ? "✓ HOOKED" : "— NOT FOUND"));
            getLogger().info("│  Timezone:       " + pad(configManager.getTimezone()));
            getLogger().info("└─────────────────────────────────────┘");

        } catch (Exception e) {
            getLogger().severe("Failed to enable RedstoneReboot: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (restartManager != null) restartManager.cleanup();
            if (serverLoadMonitor != null) serverLoadMonitor.stopMonitoring();
            if (placeholderHook != null) placeholderHook.unregister();
            if (core != null) core.onDisable();
            if (adventure != null) {
                adventure.close();
                adventure = null;
            }
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
        } finally {
            instance = null;
        }
    }

    // ── ServerPlatform implementation ──────────────────────────────

    @Override
    public void broadcastMessage(String message) {
        if (adventure != null) {
            adventure.all().sendMessage(MiniMessage.miniMessage().deserialize(message));
        }
    }

    @Override
    public void broadcastTitle(String title, String subtitle) {
        if (adventure != null) {
            adventure.all().showTitle(net.kyori.adventure.title.Title.title(
                MiniMessage.miniMessage().deserialize(title),
                MiniMessage.miniMessage().deserialize(subtitle)
            ));
        }
    }

    @Override
    public void executeConsole(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    @Override
    public double getTPS() {
        // Use reflection for cross-version compatibility (1.9-1.21.1)
        try {
            // Try Paper's Bukkit.getTPS() first via reflection
            java.lang.reflect.Method tpsMethod = Bukkit.class.getMethod("getTPS");
            double[] tps = (double[]) tpsMethod.invoke(null);
            return tps[0];
        } catch (Exception e) {
            // Fallback: NMS recentTps field
            try {
                Object server = Bukkit.getServer().getClass()
                    .getMethod("getServer").invoke(Bukkit.getServer());
                double[] recentTps = (double[]) server.getClass()
                    .getField("recentTps").get(server);
                return recentTps[0];
            } catch (Exception ex) {
                return 20.0;
            }
        }
    }

    @Override
    public String getPlatformName() {
        String brand = Bukkit.getName();
        // Detect Folia at runtime
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            brand = "Folia";
        } catch (ClassNotFoundException ignored) {}
        return brand + " (Adapter: 1.9-1.21.1)";
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
        Bukkit.getScheduler().runTaskLater(this, () -> Bukkit.getServer().shutdown(), 60L);
    }

    // ── Integration hooks ─────────────────────────────────────────

    private void hookPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null
                && configManager.isPlaceholderAPIEnabled()) {
            try {
                placeholderHook = new PlaceholderAPIHook(this);
                placeholderHook.register();
            } catch (Exception e) {
                getLogger().warning("PlaceholderAPI hook failed: " + e.getMessage());
            }
        }
    }

    // ── Utility ───────────────────────────────────────────────────

    private String pad(String text) {
        return String.format("%-20s│", text);
    }

    // ── Accessors ─────────────────────────────────────────────────

    public static RedstoneRebootPlugin getInstance() { return instance; }
    public BukkitAudiences getAdventure() { return adventure; }
    public RedstoneRebootCore getCore() { return core; }
    public ConfigManager getConfigManager() { return configManager; }
    public RestartManager getRestartManager() { return restartManager; }
    public AlertManager getAlertManager() { return alertManager; }
    public PermissionManager getPermissionManager() { return permissionManager; }
    public ServerLoadMonitor getServerLoadMonitor() { return serverLoadMonitor; }
}
