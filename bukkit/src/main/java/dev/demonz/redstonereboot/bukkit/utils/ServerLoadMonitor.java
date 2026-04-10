package dev.demonz.redstonereboot.bukkit.utils;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import dev.demonz.redstonereboot.bukkit.managers.RestartManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Real-time TPS and memory monitoring with automatic restart triggers.
 */
public class ServerLoadMonitor {
    private final RedstoneRebootPlugin plugin;
    private BukkitTask monitorTask;
    private double lastTPS = 20.0;
    private double lastMemoryUsage = 0.0;
    private int consecutiveLowTPS = 0;
    private int consecutiveHighMemory = 0;

    public ServerLoadMonitor(RedstoneRebootPlugin plugin) { this.plugin = plugin; }

    public void startMonitoring() {
        int interval = plugin.getConfigManager().getCheckInterval() * 20;
        monitorTask = new BukkitRunnable() {
            @Override public void run() { checkHealth(); }
        }.runTaskTimer(plugin, interval, interval);
        plugin.getLogger().info("Load monitoring active (interval: " + plugin.getConfigManager().getCheckInterval() + "s)");
    }

    public void stopMonitoring() {
        if (monitorTask != null) { monitorTask.cancel(); monitorTask = null; }
    }

    private void checkHealth() {
        // TPS
        try {
            java.lang.reflect.Method tpsMethod = Bukkit.class.getMethod("getTPS");
            double[] tps = (double[]) tpsMethod.invoke(null);
            lastTPS = Math.min(tps[0], 20.0);
        } catch (Exception e) {
            try {
                Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
                double[] recentTps = (double[]) server.getClass().getField("recentTps").get(server);
                lastTPS = Math.min(recentTps[0], 20.0);
            } catch (Exception ex) { lastTPS = 20.0; }
        }

        // Memory
        Runtime rt = Runtime.getRuntime();
        lastMemoryUsage = (double)(rt.totalMemory() - rt.freeMemory()) / rt.maxMemory() * 100.0;

        checkTPS();
        checkMemory();
        checkEmergency();
    }

    private void checkTPS() {
        double threshold = plugin.getConfigManager().getTpsThreshold();
        if (lastTPS < threshold) {
            consecutiveLowTPS++;
            if (consecutiveLowTPS >= plugin.getConfigManager().getConsecutiveChecks()) {
                plugin.getRestartManager().scheduleRestart(
                    plugin.getConfigManager().getEmergencyDelay(),
                    RestartManager.RestartReason.EMERGENCY_TPS, "ServerMonitor");
                consecutiveLowTPS = 0;
            }
        } else { consecutiveLowTPS = 0; }
    }

    private void checkMemory() {
        double threshold = plugin.getConfigManager().getMemoryThreshold();
        if (lastMemoryUsage > threshold) {
            consecutiveHighMemory++;
            if (consecutiveHighMemory >= plugin.getConfigManager().getConsecutiveChecks()) {
                plugin.getRestartManager().scheduleRestart(
                    plugin.getConfigManager().getEmergencyDelay(),
                    RestartManager.RestartReason.EMERGENCY_MEMORY, "ServerMonitor");
                consecutiveHighMemory = 0;
            }
        } else { consecutiveHighMemory = 0; }
    }

    private void checkEmergency() {
        if (!plugin.getConfigManager().isEmergencyRestartEnabled()) return;
        if (lastTPS < plugin.getConfigManager().getEmergencyTpsThreshold()) {
            plugin.getAlertManager().sendEmergencyAlert("Critical TPS: " + String.format("%.1f", lastTPS));
            plugin.getRestartManager().scheduleRestart(
                plugin.getConfigManager().getEmergencyDelay(),
                RestartManager.RestartReason.EMERGENCY_TPS, "EmergencyMonitor");
        }
        if (lastMemoryUsage > plugin.getConfigManager().getEmergencyMemoryThreshold()) {
            plugin.getAlertManager().sendEmergencyAlert("Critical Memory: " + String.format("%.1f%%", lastMemoryUsage));
            plugin.getRestartManager().scheduleRestart(
                plugin.getConfigManager().getEmergencyDelay(),
                RestartManager.RestartReason.EMERGENCY_MEMORY, "EmergencyMonitor");
        }
    }

    public double getLastTPS() { return lastTPS; }
    public double getLastMemoryUsage() { return lastMemoryUsage; }
    public boolean isHealthy() {
        return lastTPS >= plugin.getConfigManager().getTpsThreshold()
            && lastMemoryUsage <= plugin.getConfigManager().getMemoryThreshold();
    }
}
