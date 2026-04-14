package dev.demonz.redstonereboot.common;

import dev.demonz.redstonereboot.common.backend.BackendConfig;
import dev.demonz.redstonereboot.common.backend.BackendRegistry;
import dev.demonz.redstonereboot.common.backend.EnvironmentDetector;
import dev.demonz.redstonereboot.common.manager.RestartManager;
import dev.demonz.redstonereboot.common.platform.PlatformConfig;
import dev.demonz.redstonereboot.common.platform.ServerPlatform;
import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import dev.demonz.redstonereboot.common.text.LegacyTextUtil;
import dev.demonz.redstonereboot.common.utils.UpdateChecker;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

/**
 * Core engine for RedstoneReboot — the platform-agnostic restart orchestrator.
 * <p>
 * Initializes and manages the {@link BackendRegistry}, {@link RestartManager},
 * {@link UpdateChecker}, and environment detection. Each platform (Bukkit, Fabric,
 * Forge, NeoForge) creates a single instance and delegates lifecycle events to
 * {@link #onEnable()} and {@link #onDisable()}.
 * </p>
 *
 * @since 1.0.0
 */
public class RedstoneRebootCore {

    public static final String VERSION = "1.3.2";
    public static final String BRAND = "RedstoneReboot";

    private static final Logger LOGGER = Logger.getLogger(BRAND);

    private final ServerPlatform platform;
    private final PlatformTaskScheduler scheduler;
    private final PlatformConfig config;
    private final UpdateChecker updateChecker;
    private final BackendRegistry backendRegistry;
    private final RestartManager restartManager;

    public RedstoneRebootCore(ServerPlatform platform, PlatformTaskScheduler scheduler, PlatformConfig config, Path dataFolder) {
        this.platform = platform;
        this.scheduler = scheduler;
        this.config = config;
        this.updateChecker = new UpdateChecker("redstonereboot", VERSION, LOGGER);
        
        BackendConfig backendConfig = new BackendConfig(dataFolder, LOGGER);
        this.backendRegistry = new BackendRegistry(LOGGER, backendConfig);
        this.restartManager = new RestartManager(LOGGER, platform, scheduler, config, backendRegistry);
    }

    /**
     * Called when the platform enables the plugin or mod.
     */
    public void onEnable() {
        printStartupBanner();
        LOGGER.info("Platform: " + platform.getPlatformName() + " (MC " + platform.getMinecraftVersion() + ")");
        LOGGER.info("TPS: " + String.format("%.1f", platform.getTPS()));

        backendRegistry.initialize();
        restartManager.initialize();

        // Advisory environment detection
        List<String> detected = EnvironmentDetector.detectPotentialBackends();
        if (!detected.isEmpty()) {
            LOGGER.info("Detected Environment: " + String.join(", ", detected));
            String active = backendRegistry.getActiveBackend().getName().toUpperCase();
            if (!detected.contains(active) && !active.equals("SHUTDOWNONLY") && !active.equals("LOCALSCRIPT")) {
                LOGGER.warning("Mismatch detected: Running on " + String.join("/", detected) + " but backend is " + active);
            }
        }

        LOGGER.info("Engine initialized successfully.");
        updateChecker.checkForUpdates();
    }

    /**
     * Called when the platform disables the plugin or mod.
     */
    public void onDisable() {
        LOGGER.info("RedstoneReboot engine shutting down...");
        restartManager.cleanup();
        LOGGER.info("Shutdown complete.");
    }

    /**
     * Reload all runtime state: platform config, backend registry, and restart schedules.
     * <p>
     * Called by {@code /reboot reload} — allows VPS admins to change backend configuration
     * in {@code restart-backends.properties} without a full server restart.
     * </p>
     */
    public void reloadRuntimeState() {
        platform.reloadPlatformState();
        backendRegistry.initialize();
        restartManager.initialize();
    }

    /**
     * Trigger an emergency restart with a given reason.
     *
     * @param reason the human-readable reason for the emergency
     */
    public void triggerEmergencyRestart(String reason) {
        LOGGER.severe("==========================================");
        LOGGER.severe("EMERGENCY RESTART TRIGGERED");
        LOGGER.severe("Reason: " + reason);
        LOGGER.severe("==========================================");
        platform.sendEmergencyAlert(reason);
        int delay = config.getEmergencyDelay();
        if (delay > 0) {
            restartManager.scheduleRestart(delay, dev.demonz.redstonereboot.common.manager.RestartReason.EMERGENCY_TPS, "Emergency: " + reason);
        } else {
            restartManager.performImmediateRestart(dev.demonz.redstonereboot.common.manager.RestartReason.EMERGENCY_TPS, "Emergency: " + reason);
        }
    }

    private void printStartupBanner() {
        String[] banner = {
            "",
            "==========================================",
            "  RedstoneReboot v" + VERSION,
            "  by DemonZ Development",
            "------------------------------------------",
            "  Platform  : " + platform.getPlatformName(),
            "  Minecraft : " + platform.getMinecraftVersion(),
            "  Players   : " + platform.getOnlinePlayerCount(),
            "  Engine    : Multi-Platform Restart Engine",
            "==========================================",
            ""
        };

        for (String line : banner) {
            LOGGER.info(LegacyTextUtil.stripLegacyFormatting(line));
        }
    }

    /** @return the platform abstraction for the current server environment */
    public ServerPlatform getPlatform() {
        return platform;
    }

    /** @return the update checker that polls Modrinth for new versions */
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    /** @return the central restart manager handling scheduling, countdowns, and execution */
    public RestartManager getRestartManager() {
        return restartManager;
    }

    /** @return the backend registry managing the active restart backend */
    public BackendRegistry getBackendRegistry() {
        return backendRegistry;
    }

    /** @return the platform task scheduler used for tick-based scheduling */
    public PlatformTaskScheduler getScheduler() {
        return scheduler;
    }

    /** @return the platform configuration providing scheduling, monitoring, and emergency settings */
    public PlatformConfig getConfig() {
        return config;
    }
}
